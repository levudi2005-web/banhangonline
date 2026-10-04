package com.banhangonline.auth.service;

import com.banhangonline.auth.dto.OtpSendRequest;
import com.banhangonline.auth.dto.OtpVerificationResponse;
import com.banhangonline.auth.dto.OtpVerifyRequest;
import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.auth.entity.OtpPurpose;
import com.banhangonline.auth.entity.VerificationCode;
import com.banhangonline.auth.repository.VerificationCodeRepository;
import com.banhangonline.auth.security.TokenUtil;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.config.AppProperties;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.user.repository.UserRepository;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ACCEPTED_MESSAGE = "Nếu tài khoản tồn tại, mã xác minh sẽ được gửi.";
    private final UserRepository users;
    private final VerificationCodeRepository codes;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final Map<OtpChannel, OtpSender> senders = new EnumMap<>(OtpChannel.class);
    private final PasswordResetService passwordResets;

    public OtpService(
            UserRepository users,
            VerificationCodeRepository codes,
            PasswordEncoder encoder,
            AppProperties props,
            List<OtpSender> senderList,
            PasswordResetService passwordResets) {
        this.users = users;
        this.codes = codes;
        this.encoder = encoder;
        this.props = props;
        this.passwordResets = passwordResets;
        for (OtpSender sender : senderList) {
            if (senders.put(sender.channel(), sender) != null) {
                throw new IllegalStateException("Multiple OTP senders configured for " + sender.channel());
            }
        }
    }

    @Transactional
    public String send(OtpSendRequest request, String ipAddress) {
        validatePurposeChannel(request.purpose(), request.channel());
        String destination = normalizeDestination(request.channel(), request.destination());
        OtpSender sender = sender(request.channel());
        sender.ensureConfigured();

        String destinationHash = destinationHash(destination);
        Instant now = Instant.now();
        Optional<User> found = users.findByEmail(destination);
        if (found.isEmpty() || !eligible(found.get(), request.purpose())) {
            return ACCEPTED_MESSAGE;
        }

        User user = found.get();
        enforceSendLimits(user, request, destinationHash, ipAddress, now);
        codes.invalidateActive(user.getId(), request.channel(), request.purpose(), destinationHash, now);

        String rawCode = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        VerificationCode verification = new VerificationCode();
        verification.setUser(user);
        verification.setChannel(request.channel());
        verification.setPurpose(request.purpose());
        verification.setDestinationHash(destinationHash);
        verification.setCodeHash(encoder.encode(rawCode));
        verification.setIpAddress(sanitizeIp(ipAddress));
        verification.setExpiresAt(now.plusSeconds(props.otp().expirationSeconds()));
        verification.setAttempts(0);
        verification.setMaxAttempts(props.otp().maxAttempts());
        verification.setCreatedAt(now);
        codes.saveAndFlush(verification);

        sender.send(destination, rawCode, props.otp().expirationSeconds());
        return ACCEPTED_MESSAGE;
    }

    @Transactional
    public OtpVerificationResponse verify(OtpVerifyRequest request) {
        validatePurposeChannel(request.purpose(), request.channel());
        String destination = normalizeDestination(request.channel(), request.destination());
        String destinationHash = destinationHash(destination);
        Optional<User> found = users.findByEmail(destination);
        ApiException invalid = invalidCode();
        if (found.isEmpty() || !eligible(found.get(), request.purpose())) {
            throw invalid;
        }
        User user = found.get();
        VerificationCode code = codes
                .findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), request.channel(), request.purpose(), destinationHash)
                .orElseThrow(() -> invalid);

        Instant now = Instant.now();
        if (!code.getExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "Mã xác minh đã hết hạn. Hãy gửi mã mới.");
        }
        if (codes.incrementAttemptsIfActive(code.getId(), now) == 0) {
            if (code.getAttempts() >= code.getMaxAttempts()) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS",
                        "Bạn đã nhập sai quá số lần cho phép. Hãy gửi mã mới.");
            }
            throw invalid;
        }
        if (!encoder.matches(request.code(), code.getCodeHash())) {
            throw invalid;
        }
        if (codes.markUsedIfActive(code.getId(), now) == 0) {
            throw invalid;
        }

        String resetToken = null;
        if (request.purpose() == OtpPurpose.RESET_PASSWORD) {
            resetToken = passwordResets.issueAfterOtp(user);
        } else {
            user.setEmailVerified(true);
            if (user.hasRole("CUSTOMER") && user.isEmailVerified()) {
                user.setStatus(UserStatus.ACTIVE);
            }
        }
        codes.invalidateActive(user.getId(), request.channel(), request.purpose(), destinationHash, now);
        return new OtpVerificationResponse(true, resetToken);
    }

    private void enforceSendLimits(
            User user, OtpSendRequest request, String destinationHash, String ipAddress, Instant now) {
        var latest = codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashOrderByCreatedAtDesc(
                user.getId(), request.channel(), request.purpose(), destinationHash);
        if (latest.isPresent()
                && latest.get().getCreatedAt().plusSeconds(props.otp().resendCooldownSeconds()).isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN",
                    "Vui lòng đợi trước khi yêu cầu mã mới.");
        }
        if (codes.countByDestinationHashAndPurposeAndCreatedAtAfter(
                        destinationHash, request.purpose(), now.minusSeconds(3_600))
                >= props.otp().maxSendsPerHour()
                || codes.countByDestinationHashAndPurposeAndCreatedAtAfter(
                                destinationHash, request.purpose(), now.minusSeconds(86_400))
                        >= props.otp().maxSendsPerDay()
                || codes.countByIpAddressAndCreatedAtAfter(sanitizeIp(ipAddress), now.minusSeconds(3_600))
                        >= props.otp().maxSendsPerHour()
                || codes.countByIpAddressAndCreatedAtAfter(sanitizeIp(ipAddress), now.minusSeconds(86_400))
                        >= props.otp().maxSendsPerDay()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED",
                    "Đã vượt quá số lần yêu cầu mã xác minh. Vui lòng thử lại sau.");
        }
    }

    private boolean eligible(User user, OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER_EMAIL -> user.getStatus() == UserStatus.PENDING_VERIFICATION && !user.isEmailVerified();
            case RESET_PASSWORD -> user.getStatus() == UserStatus.ACTIVE;
        };
    }

    private void validatePurposeChannel(OtpPurpose purpose, OtpChannel channel) {
        if (channel != OtpChannel.EMAIL) {
            throw ApiException.validation("Kênh xác minh không hợp lệ cho mục đích đã chọn");
        }
    }

    private String normalizeDestination(OtpChannel channel, String raw) {
        if (!StringUtils.hasText(raw)) {
            throw ApiException.validation("Địa chỉ nhận mã không hợp lệ");
        }
        return Rules.email(raw);
    }

    private String destinationHash(String destination) {
        String secret = props.otp().hashSecret();
        if (!StringUtils.hasText(secret)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OTP_CONFIGURATION_UNAVAILABLE",
                    "Dịch vụ xác minh hiện chưa được cấu hình.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(destination.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        }
    }

    private OtpSender sender(OtpChannel channel) {
        OtpSender sender = senders.get(channel);
        if (sender == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OTP_DELIVERY_UNAVAILABLE",
                    "Kênh gửi mã xác minh hiện chưa khả dụng.");
        }
        return sender;
    }

    private String sanitizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        return ipAddress.length() <= 45 ? ipAddress : ipAddress.substring(0, 45);
    }

    private ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID",
                "Mã OTP không chính xác hoặc đã được sử dụng. Hãy kiểm tra và thử lại.");
    }
}

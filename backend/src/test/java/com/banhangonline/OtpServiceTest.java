package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banhangonline.auth.dto.OtpSendRequest;
import com.banhangonline.auth.dto.OtpVerifyRequest;
import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.auth.entity.OtpPurpose;
import com.banhangonline.auth.entity.VerificationCode;
import com.banhangonline.auth.repository.VerificationCodeRepository;
import com.banhangonline.auth.service.OtpSender;
import com.banhangonline.auth.service.OtpService;
import com.banhangonline.auth.service.PasswordResetService;
import com.banhangonline.auth.security.TokenUtil;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.config.AppProperties;
import com.banhangonline.role.entity.Role;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class OtpServiceTest {
    private static final String HASH_SECRET = TokenUtil.newToken() + TokenUtil.newToken();
    private static final String EMAIL = "new-user@example.com";
    private final UserRepository users = mock(UserRepository.class);
    private final VerificationCodeRepository codes = mock(VerificationCodeRepository.class);
    private final PasswordResetService resets = mock(PasswordResetService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final CapturingSender sender = new CapturingSender();
    private final OtpService service = new OtpService(
            users, codes, encoder, properties(), List.of(sender), resets);

    @Test
    void sendStoresOnlyBcryptHashAndInvalidatesPreviousCode() {
        User user = user();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        String responseMessage = service.send(
                new OtpSendRequest(OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL), "127.0.0.1");

        assertThat(sender.code).matches("\\d{6}");
        assertThat(responseMessage).doesNotContain(sender.code);
        var captor = org.mockito.ArgumentCaptor.forClass(VerificationCode.class);
        verify(codes).saveAndFlush(captor.capture());
        VerificationCode saved = captor.getValue();
        assertThat(saved.getCodeHash()).isNotEqualTo(sender.code);
        assertThat(encoder.matches(sender.code, saved.getCodeHash())).isTrue();
        assertThat(saved.getDestinationHash()).hasSize(64).isNotEqualTo(EMAIL);
        verify(codes).invalidateActive(eq(user.getId()), eq(OtpChannel.EMAIL),
                eq(OtpPurpose.REGISTER_EMAIL), anyString(), any(Instant.class));
    }

    @Test
    void verifyConsumesCodeAndMarksRegistrationEmailVerified() {
        User user = user();
        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        user.getRoles().add(customerRole);
        VerificationCode code = code(user, "123456");
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.REGISTER_EMAIL), anyString()))
                .thenReturn(Optional.of(code));
        when(codes.incrementAttemptsIfActive(eq(7L), any(Instant.class))).thenReturn(1);
        when(codes.markUsedIfActive(eq(7L), any(Instant.class))).thenReturn(1);

        var result = service.verify(new OtpVerifyRequest(
                OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL, "123456"));

        assertThat(result.verified()).isTrue();
        assertThat(result.resetToken()).isNull();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(codes).markUsedIfActive(eq(7L), any(Instant.class));
    }

    @Test
    void invalidCodeConsumesAnAttemptAndDoesNotVerifyUser() {
        User user = user();
        VerificationCode code = code(user, "123456");
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.REGISTER_EMAIL), anyString()))
                .thenReturn(Optional.of(code));
        when(codes.incrementAttemptsIfActive(eq(7L), any(Instant.class))).thenReturn(1);

        assertThatThrownBy(() -> service.verify(new OtpVerifyRequest(
                        OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL, "999999")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Mã OTP");
        assertThat(user.isEmailVerified()).isFalse();
        verify(codes, never()).markUsedIfActive(any(), any());
    }

    @Test
    void expiredCodeCannotBeUsed() {
        User user = user();
        VerificationCode code = code(user, "123456");
        code.setExpiresAt(Instant.now().minusSeconds(1));
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.REGISTER_EMAIL), anyString()))
                .thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.verify(new OtpVerifyRequest(
                        OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL, "123456")))
                .isInstanceOf(ApiException.class);
        verify(codes, never()).incrementAttemptsIfActive(any(), any());
    }

    @Test
    void usedCodeCannotBeUsedAgain() {
        User user = user();
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.REGISTER_EMAIL), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(new OtpVerifyRequest(
                        OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL, "123456")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo("OTP_INVALID"));
    }

    @Test
    void maximumAttemptsRequiresAtomicIncrementToHaveReachedItsLimit() {
        User user = user();
        VerificationCode code = code(user, "123456");
        code.setAttempts(5);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.REGISTER_EMAIL), anyString()))
                .thenReturn(Optional.of(code));
        when(codes.incrementAttemptsIfActive(eq(7L), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.verify(new OtpVerifyRequest(
                        OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL, "123456")))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo("OTP_MAX_ATTEMPTS"));
        verify(codes, never()).markUsedIfActive(any(), any());
    }

    @Test
    void resendIsBlockedDuringCooldown() {
        User user = user();
        VerificationCode latest = new VerificationCode();
        latest.setCreatedAt(Instant.now());
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.REGISTER_EMAIL), anyString()))
                .thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.send(
                        new OtpSendRequest(OtpChannel.EMAIL, OtpPurpose.REGISTER_EMAIL, EMAIL), "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("đợi");
        verify(codes, never()).saveAndFlush(any());
    }

    @Test
    void resetOtpIssuesExistingOneTimeResetToken() {
        User user = user();
        user.setStatus(UserStatus.ACTIVE);
        VerificationCode code = code(user, "123456");
        code.setPurpose(OtpPurpose.RESET_PASSWORD);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.RESET_PASSWORD), anyString()))
                .thenReturn(Optional.of(code));
        when(codes.incrementAttemptsIfActive(eq(7L), any(Instant.class))).thenReturn(1);
        when(codes.markUsedIfActive(eq(7L), any(Instant.class))).thenReturn(1);
        String resetToken = TokenUtil.newToken();
        when(resets.issueAfterOtp(user)).thenReturn(resetToken);

        var result = service.verify(new OtpVerifyRequest(
                OtpChannel.EMAIL, OtpPurpose.RESET_PASSWORD, EMAIL, "123456"));

        assertThat(result.verified()).isTrue();
        assertThat(result.resetToken()).isEqualTo(resetToken);
        verify(resets).issueAfterOtp(user);
    }

    @Test
    void sendingIsRateLimitedByDestination() {
        User user = user();
        user.setStatus(UserStatus.ACTIVE);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndChannelAndPurposeAndDestinationHashOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpChannel.EMAIL), eq(OtpPurpose.RESET_PASSWORD), anyString()))
                .thenReturn(Optional.empty());
        when(codes.countByDestinationHashAndPurposeAndCreatedAtAfter(
                anyString(), eq(OtpPurpose.RESET_PASSWORD), any(Instant.class)))
                .thenReturn(5L);

        assertThatThrownBy(() -> service.send(
                        new OtpSendRequest(OtpChannel.EMAIL, OtpPurpose.RESET_PASSWORD, EMAIL), "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo("OTP_RATE_LIMITED"));
        verify(codes, never()).saveAndFlush(any());
    }

    @Test
    void repositoryDerivedQueriesResolveAgainstEntityProperties() {
        assertThatCode(() -> new PartTree(
                        "findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc",
                        VerificationCode.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> new PartTree(
                        "findTopByUserIdAndChannelAndPurposeAndDestinationHashOrderByCreatedAtDesc",
                        VerificationCode.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> new PartTree(
                        "countByDestinationHashAndPurposeAndCreatedAtAfter",
                        VerificationCode.class))
                .doesNotThrowAnyException();
    }

    @Test
    void failedAttemptsAreCommittedOutsideTheVerificationTransaction() throws Exception {
        var method = VerificationCodeRepository.class.getMethod(
                "incrementAttemptsIfActive", Long.class, Instant.class);

        assertThat(method.getAnnotation(Transactional.class).propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void emailIsTheOnlySupportedOtpChannel() {
        assertThat(OtpChannel.values()).containsExactly(OtpChannel.EMAIL);
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setEmail(EMAIL);
        user.setPhone("0901234567");
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        return user;
    }

    private VerificationCode code(User user, String rawCode) {
        VerificationCode code = new VerificationCode();
        code.setId(7L);
        code.setUser(user);
        code.setChannel(OtpChannel.EMAIL);
        code.setPurpose(OtpPurpose.REGISTER_EMAIL);
        code.setDestinationHash("0".repeat(64));
        code.setCodeHash(encoder.encode(rawCode));
        code.setExpiresAt(Instant.now().plusSeconds(300));
        code.setAttempts(0);
        code.setMaxAttempts(5);
        return code;
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Cookie("SID", true, "Lax"),
                new AppProperties.Cors(List.of()),
                new AppProperties.Session(8, 30),
                new AppProperties.ResetToken(30),
                new AppProperties.RateLimit(10, 60),
                new AppProperties.Otp(300, 5, 60, 5, 10, HASH_SECRET));
    }

    private static class CapturingSender implements OtpSender {
        private String code;

        @Override public OtpChannel channel() { return OtpChannel.EMAIL; }
        @Override public void ensureConfigured() {}
        @Override public void send(String destination, String sentCode, int expirationSeconds) { code = sentCode; }
    }
}

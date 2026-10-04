package com.banhangonline.auth.service;

import com.banhangonline.config.AppProperties;
import com.banhangonline.auth.entity.PasswordResetToken;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.auth.repository.PasswordResetTokenRepository;
import com.banhangonline.auth.repository.SessionRepository;
import com.banhangonline.auth.security.TokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class PasswordResetService {
    private final PasswordResetTokenRepository tokens;
    private final SessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public PasswordResetService(PasswordResetTokenRepository tokens, SessionRepository sessions,
                                PasswordEncoder encoder, AppProperties props) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.encoder = encoder;
        this.props = props;
    }

    @Transactional
    public String issueAfterOtp(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP",
                    "Mã xác minh không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
        }
        tokens.deleteByUser(user);
        String raw = TokenUtil.newToken();
        Instant now = Instant.now();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(TokenUtil.sha256(raw));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusSeconds(props.resetToken().ttlMinutes() * 60L));
        tokens.saveAndFlush(token);
        return raw;
    }

    @Transactional
    public void reset(String rawToken, String newPassword, String confirm) {
        Rules.password(newPassword, confirm);
        ApiException invalid = new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        PasswordResetToken t = tokens.findByTokenHash(TokenUtil.sha256(rawToken)).orElseThrow(() -> invalid);
        if (t.getUsedAt() != null || !t.getExpiresAt().isAfter(Instant.now())) throw invalid;
        if (tokens.markUsed(t.getId(), Instant.now()) == 0) throw invalid; // dùng một lần, an toàn khi gọi song song
        User u = t.getUser();
        u.setPasswordHash(encoder.encode(newPassword));
        sessions.deleteByUser(u); // đăng xuất mọi thiết bị
    }
}

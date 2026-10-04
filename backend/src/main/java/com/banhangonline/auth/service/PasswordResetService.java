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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final AuthService auth;
    private final PasswordResetTokenRepository tokens;
    private final SessionRepository sessions;
    private final PasswordEncoder encoder;
    private final ResetNotifier notifier;
    private final AppProperties props;

    public PasswordResetService(AuthService auth, PasswordResetTokenRepository tokens, SessionRepository sessions,
                                PasswordEncoder encoder, ResetNotifier notifier, AppProperties props) {
        this.auth = auth;
        this.tokens = tokens;
        this.sessions = sessions;
        this.encoder = encoder;
        this.notifier = notifier;
        this.props = props;
    }

    /** Luôn trả về như nhau dù tài khoản có tồn tại hay không. */
    @Transactional
    public void request(String account) {
        notifier.ensureConfigured();
        Optional<User> found = auth.findByAccount(account);
        if (found.isEmpty() || found.get().getStatus() != UserStatus.ACTIVE) return;
        User u = found.get();
        tokens.deleteByUser(u);
        String raw = TokenUtil.newToken();
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(u);
        t.setTokenHash(TokenUtil.sha256(raw));
        t.setCreatedAt(Instant.now());
        t.setExpiresAt(Instant.now().plusSeconds(props.resetToken().ttlMinutes() * 60L));
        tokens.saveAndFlush(t);
        try {
            notifier.send(u, raw);
        } catch (ApiException e) {
            if (!"EMAIL_DELIVERY_FAILED".equals(e.getCode())) throw e;
            tokens.delete(t);
            log.warn("Password reset notification failed; keeping the account-enumeration-safe response");
        }
    }

    @Transactional
    public void reset(String rawToken, String newPassword, String confirm) {
        Rules.password(newPassword, confirm);
        ApiException invalid = new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        PasswordResetToken t = tokens.findByTokenHash(TokenUtil.sha256(rawToken)).orElseThrow(() -> invalid);
        if (t.getUsedAt() != null || t.getExpiresAt().isBefore(Instant.now())) throw invalid;
        if (tokens.markUsed(t.getId(), Instant.now()) == 0) throw invalid; // dùng một lần, an toàn khi gọi song song
        User u = t.getUser();
        u.setPasswordHash(encoder.encode(newPassword));
        sessions.deleteByUser(u); // đăng xuất mọi thiết bị
    }
}

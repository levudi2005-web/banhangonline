package com.banhangonline.auth.service;

import com.banhangonline.config.AppProperties;
import com.banhangonline.auth.entity.Session;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.auth.repository.SessionRepository;
import com.banhangonline.auth.security.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class SessionService {
    public record Issued(String token, Duration ttl) {}

    private final SessionRepository sessions;
    private final AppProperties props;

    public SessionService(SessionRepository sessions, AppProperties props) {
        this.sessions = sessions;
        this.props = props;
    }

    @Transactional
    public Issued create(User user, boolean remember, String ip, String userAgent) {
        Duration ttl = remember ? Duration.ofDays(props.session().rememberDays()) : Duration.ofHours(props.session().ttlHours());
        String token = TokenUtil.newToken();
        Session s = new Session();
        s.setTokenHash(TokenUtil.sha256(token));
        s.setUser(user);
        s.setCreatedAt(Instant.now());
        s.setExpiresAt(Instant.now().plus(ttl));
        s.setIpAddress(ip);
        s.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length())));
        sessions.save(s);
        return new Issued(token, ttl);
    }

    @Transactional
    public Optional<User> resolve(String token) {
        Optional<Session> found = sessions.findByTokenHash(TokenUtil.sha256(token));
        if (found.isEmpty()) return Optional.empty();
        Session s = found.get();
        if (s.getExpiresAt().isBefore(Instant.now())) {
            sessions.delete(s);
            return Optional.empty();
        }
        return s.getUser().getStatus() == UserStatus.ACTIVE ? Optional.of(s.getUser()) : Optional.empty();
    }

    @Transactional
    public void revoke(String token) {
        sessions.deleteByTokenHash(TokenUtil.sha256(token));
    }
}

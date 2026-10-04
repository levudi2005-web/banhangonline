package com.banhangonline.auth.service;

import com.banhangonline.auth.repository.PasswordResetTokenRepository;
import com.banhangonline.auth.repository.SessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
public class MaintenanceTask {
    private final SessionRepository sessions;
    private final PasswordResetTokenRepository tokens;

    public MaintenanceTask(SessionRepository sessions, PasswordResetTokenRepository tokens) {
        this.sessions = sessions;
        this.tokens = tokens;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    @Transactional
    public void purgeExpired() {
        sessions.deleteByExpiresAtBefore(Instant.now());
        tokens.deleteByExpiresAtBefore(Instant.now().minusSeconds(86_400));
    }
}

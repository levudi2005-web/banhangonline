package com.banhangonline.auth.repository;

import com.banhangonline.auth.entity.Session;
import com.banhangonline.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByTokenHash(String tokenHash);
    void deleteByTokenHash(String tokenHash);
    void deleteByUser(User user);
    void deleteByExpiresAtBefore(Instant time);
}

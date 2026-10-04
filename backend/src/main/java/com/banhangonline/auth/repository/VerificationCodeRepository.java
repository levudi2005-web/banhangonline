package com.banhangonline.auth.repository;

import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.auth.entity.OtpPurpose;
import com.banhangonline.auth.entity.VerificationCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByUserIdAndChannelAndPurposeAndDestinationHashAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            Long userId, OtpChannel channel, OtpPurpose purpose, String destinationHash);

    Optional<VerificationCode> findTopByUserIdAndChannelAndPurposeAndDestinationHashOrderByCreatedAtDesc(
            Long userId, OtpChannel channel, OtpPurpose purpose, String destinationHash);

    long countByDestinationHashAndPurposeAndCreatedAtAfter(
            String destinationHash, OtpPurpose purpose, Instant createdAt);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, Instant createdAt);

    @Modifying
    @Query("""
            update VerificationCode c
               set c.invalidatedAt = :now
             where c.user.id = :userId
               and c.channel = :channel
               and c.purpose = :purpose
               and c.destinationHash = :destinationHash
               and c.usedAt is null
               and c.invalidatedAt is null
            """)
    int invalidateActive(
            @Param("userId") Long userId,
            @Param("channel") OtpChannel channel,
            @Param("purpose") OtpPurpose purpose,
            @Param("destinationHash") String destinationHash,
            @Param("now") Instant now);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("""
            update VerificationCode c
               set c.attempts = c.attempts + 1
             where c.id = :id
               and c.usedAt is null
               and c.invalidatedAt is null
               and c.attempts < c.maxAttempts
               and c.expiresAt > :now
            """)
    int incrementAttemptsIfActive(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("""
            update VerificationCode c
               set c.usedAt = :now
             where c.id = :id
               and c.usedAt is null
               and c.invalidatedAt is null
               and c.attempts <= c.maxAttempts
               and c.expiresAt > :now
            """)
    int markUsedIfActive(@Param("id") Long id, @Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant expiresAt);
}

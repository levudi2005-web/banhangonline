package com.banhangonline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cookie cookie, Cors cors, Session session, ResetToken resetToken, RateLimit rateLimit, Otp otp) {
    public record Cookie(String name, boolean secure, String sameSite) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Session(int ttlHours, int rememberDays) {}
    public record ResetToken(int ttlMinutes) {}
    public record RateLimit(int maxAttempts, int windowSeconds) {}
    public record Otp(
            int expirationSeconds,
            int maxAttempts,
            int resendCooldownSeconds,
            int maxSendsPerHour,
            int maxSendsPerDay,
            String hashSecret) {}
}

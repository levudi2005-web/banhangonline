package com.banhangonline.auth.service;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.entity.User;
import jakarta.mail.internet.InternetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SmtpResetNotifier implements ResetNotifier {
    private static final Logger log = LoggerFactory.getLogger(SmtpResetNotifier.class);
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String smtpHost;
    private final String username;
    private final String password;
    private final String from;
    private final String frontendBaseUrl;
    private final boolean tlsEnabled;

    public SmtpResetNotifier(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${SMTP_HOST:}") String smtpHost,
            @Value("${SMTP_USERNAME:}") String username,
            @Value("${SMTP_PASSWORD:}") String password,
            @Value("${SMTP_FROM:}") String from,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${SMTP_TLS_ENABLED:true}") boolean tlsEnabled) {
        this.mailSenders = mailSenders;
        this.smtpHost = smtpHost;
        this.username = username;
        this.password = password;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl;
        this.tlsEnabled = tlsEnabled;
    }

    @Override
    public void ensureConfigured() {
        if (mailSenders.getIfAvailable() == null
                || !StringUtils.hasText(smtpHost)
                || !StringUtils.hasText(username)
                || !StringUtils.hasText(password)
                || !StringUtils.hasText(from)
                || !tlsEnabled
                || !isSecureFrontendBaseUrl(frontendBaseUrl)) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DELIVERY_UNAVAILABLE",
                    "Chức năng đặt lại mật khẩu hiện chưa khả dụng. Vui lòng thử lại sau.");
        }
        try {
            new InternetAddress(from).validate();
        } catch (Exception e) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DELIVERY_UNAVAILABLE",
                    "Chức năng đặt lại mật khẩu hiện chưa khả dụng. Vui lòng thử lại sau.");
        }
    }

    private boolean isSecureFrontendBaseUrl(String value) {
        try {
            URI uri = URI.create(value);
            boolean secureScheme = "https".equalsIgnoreCase(uri.getScheme());
            boolean localDevelopment = "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            return uri.isAbsolute() && uri.getHost() != null && (secureScheme || localDevelopment)
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void send(User user, String rawToken) {
        ensureConfigured();
        boolean staff = user.hasRole("STAFF") || user.hasRole("OWNER");
        String page = staff
                ? "/pages/staff/auth/reset-password.html"
                : "/pages/customer/auth/reset-password.html";
        String link = frontendBaseUrl.replaceAll("/+$", "")
                + page + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Đặt lại mật khẩu");
        message.setText("Mở liên kết sau để đặt lại mật khẩu. Liên kết chỉ dùng được một lần và sẽ hết hạn:\n\n"
                + link + "\n\nNếu bạn không yêu cầu thay đổi mật khẩu, hãy bỏ qua email này.");
        try {
            mailSenders.getObject().send(message);
        } catch (MailException e) {
            log.warn("Password reset email delivery failed ({})", e.getClass().getSimpleName());
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DELIVERY_FAILED",
                    "Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau.");
        }
    }
}

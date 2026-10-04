package com.banhangonline.auth.service;

import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.common.exception.ApiException;
import jakarta.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SmtpOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpOtpSender.class);
    private static final String GMAIL_SMTP_HOST = "smtp.gmail.com";
    private static final int GMAIL_SMTP_PORT = 587;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String host;
    private final String username;
    private final String password;
    private final String from;
    private final int port;
    private final boolean tlsEnabled;

    public SmtpOtpSender(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${SMTP_HOST:smtp.gmail.com}") String host,
            @Value("${SMTP_PORT:587}") int port,
            @Value("${SMTP_USERNAME:}") String username,
            @Value("${SMTP_PASSWORD:}") String password,
            @Value("${SMTP_FROM:}") String from,
            @Value("${SMTP_TLS_ENABLED:true}") boolean tlsEnabled) {
        this.mailSenders = mailSenders;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
        this.tlsEnabled = tlsEnabled;
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void ensureConfigured() {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailSender == null || !GMAIL_SMTP_HOST.equalsIgnoreCase(host)
                || port != GMAIL_SMTP_PORT
                || !StringUtils.hasText(username) || !StringUtils.hasText(password)
                || !StringUtils.hasText(from) || !tlsEnabled) {
            throw unavailable("EMAIL_DELIVERY_UNAVAILABLE", "Email OTP hiện chưa khả dụng.");
        }
        if (mailSender instanceof JavaMailSenderImpl configuredSender
                && !matchesRuntimeConfiguration(configuredSender)) {
            throw unavailable("EMAIL_DELIVERY_UNAVAILABLE", "Cấu hình gửi email hiện chưa khả dụng.");
        }
        try {
            new InternetAddress(from).validate();
        } catch (Exception e) {
            throw unavailable("EMAIL_DELIVERY_UNAVAILABLE", "Email OTP hiện chưa khả dụng.");
        }
    }

    @Override
    public void send(String destination, String code, int expirationSeconds) {
        ensureConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(destination);
        message.setSubject("Mã xác minh");
        message.setText("Mã xác minh của bạn là " + code + ". Mã hết hạn sau "
                + Math.max(1, (expirationSeconds + 59) / 60)
                + " phút. Không chia sẻ mã này với bất kỳ ai.");
        try {
            mailSenders.getObject().send(message);
        } catch (MailException e) {
            log.warn("Email OTP delivery failed: {}", diagnostic(e, destination, code));
            throw unavailable("EMAIL_DELIVERY_FAILED", "Không thể gửi mã xác minh qua email.");
        }
    }

    private boolean matchesRuntimeConfiguration(JavaMailSenderImpl sender) {
        Properties properties = sender.getJavaMailProperties();
        return GMAIL_SMTP_HOST.equalsIgnoreCase(sender.getHost())
                && sender.getPort() == GMAIL_SMTP_PORT
                && username.equals(sender.getUsername())
                && password.equals(sender.getPassword())
                && "true".equalsIgnoreCase(properties.getProperty("mail.smtp.auth"))
                && "true".equalsIgnoreCase(properties.getProperty("mail.smtp.starttls.enable"))
                && "true".equalsIgnoreCase(properties.getProperty("mail.smtp.starttls.required"))
                && "true".equalsIgnoreCase(properties.getProperty("mail.smtp.ssl.checkserveridentity"))
                && "10000".equals(properties.getProperty("mail.smtp.connectiontimeout"))
                && "10000".equals(properties.getProperty("mail.smtp.timeout"))
                && "10000".equals(properties.getProperty("mail.smtp.writetimeout"));
    }

    private String diagnostic(MailException error, String destination, String code) {
        List<Throwable> exceptions = new ArrayList<>();
        addCauseChain(error, exceptions, Collections.newSetFromMap(new IdentityHashMap<>()));
        if (error instanceof org.springframework.mail.MailSendException sendException) {
            for (Exception failedMessage : sendException.getFailedMessages().values()) {
                addCauseChain(failedMessage, exceptions, Collections.newSetFromMap(new IdentityHashMap<>()));
            }
        }

        Throwable root = deepestCause(error);
        if (root == error && !exceptions.isEmpty()) {
            root = deepestCause(exceptions.get(1 < exceptions.size() ? 1 : 0));
        }
        String chain = exceptions.stream()
                .map(this::formatThrowable)
                .map(value -> redact(value, destination, code))
                .reduce((left, right) -> left + " -> " + right)
                .orElse(formatThrowable(error));
        return "exceptionClass=" + error.getClass().getName()
                + ", message=" + redact(error.getMessage(), destination, code)
                + ", rootCauseClass=" + root.getClass().getName()
                + ", rootCauseMessage=" + redact(root.getMessage(), destination, code)
                + ", causeChain=" + chain;
    }

    private void addCauseChain(Throwable error, List<Throwable> result, Set<Throwable> visited) {
        Throwable current = error;
        while (current != null && visited.add(current)) {
            result.add(current);
            current = current.getCause();
        }
    }

    private Throwable deepestCause(Throwable error) {
        Throwable current = error;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current.getCause() != null && visited.add(current)) {
            current = current.getCause();
        }
        return current;
    }

    private String formatThrowable(Throwable error) {
        return error.getClass().getName() + ": " + String.valueOf(error.getMessage());
    }

    private String redact(String message, String destination, String code) {
        String sanitized = String.valueOf(message);
        for (String secret : List.of(username, password, from, destination, code)) {
            if (StringUtils.hasText(secret)) {
                sanitized = sanitized.replace(secret, "[REDACTED]");
            }
        }
        return sanitized;
    }

    private ApiException unavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}

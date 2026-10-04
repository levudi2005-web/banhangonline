package com.banhangonline.auth.service;

import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.common.exception.ApiException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
        if (mailSenders.getIfAvailable() == null || !GMAIL_SMTP_HOST.equalsIgnoreCase(host)
                || port != GMAIL_SMTP_PORT
                || !StringUtils.hasText(username) || !StringUtils.hasText(password)
                || !StringUtils.hasText(from) || !tlsEnabled) {
            throw unavailable("EMAIL_DELIVERY_UNAVAILABLE", "Email OTP hiện chưa khả dụng.");
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
            log.warn("Email OTP delivery failed ({})", e.getClass().getSimpleName());
            throw unavailable("EMAIL_DELIVERY_FAILED", "Không thể gửi mã xác minh qua email.");
        }
    }

    private ApiException unavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}

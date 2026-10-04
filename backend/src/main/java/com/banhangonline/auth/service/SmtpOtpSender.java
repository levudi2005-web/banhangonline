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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SmtpOtpSender implements OtpSender {
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String host;
    private final String username;
    private final String password;
    private final String from;
    private final boolean tlsEnabled;

    public SmtpOtpSender(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${SMTP_HOST:}") String host,
            @Value("${SMTP_USERNAME:}") String username,
            @Value("${SMTP_PASSWORD:}") String password,
            @Value("${SMTP_FROM:}") String from,
            @Value("${SMTP_TLS_ENABLED:true}") boolean tlsEnabled) {
        this.mailSenders = mailSenders;
        this.host = host;
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
        if (mailSenders.getIfAvailable() == null || !StringUtils.hasText(host)
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
            throw unavailable("EMAIL_DELIVERY_FAILED", "Không thể gửi mã xác minh qua email.");
        }
    }

    private ApiException unavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}

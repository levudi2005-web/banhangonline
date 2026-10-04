package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.banhangonline.auth.service.SmtpOtpSender;
import com.banhangonline.common.exception.ApiException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class OtpSenderConfigurationTest {
    @Test
    void sendsOtpThroughConfiguredGmailSmtpSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpOtpSender sender = configuredSender(mailSender, true);

        sender.send("customer@gmail.com", "042671", 300);

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("customer@gmail.com");
        assertThat(message.getValue().getText()).contains("042671");
        assertThat(message.getValue().getText()).contains("5 phút");
    }

    @Test
    void reportsSmtpDeliveryFailureWithoutExposingProviderDetails() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("sensitive provider diagnostics"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        SmtpOtpSender sender = configuredSender(mailSender, true);

        assertThatThrownBy(() -> sender.send("customer@gmail.com", "042671", 300))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException apiError = (ApiException) error;
                    assertThat(apiError.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiError.getCode()).isEqualTo("EMAIL_DELIVERY_FAILED");
                    assertThat(apiError.getMessage()).doesNotContain("sensitive provider diagnostics");
                });
    }

    @Test
    void failsExplicitlyWhenSmtpCredentialsAreMissing() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("mailSender", mock(JavaMailSender.class));
        SmtpOtpSender sender = new SmtpOtpSender(
                factory.getBeanProvider(JavaMailSender.class),
                "smtp.gmail.com", 587, "", "", "", true);

        assertThatThrownBy(sender::ensureConfigured)
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void failsExplicitlyWhenSmtpSenderBeanIsUnavailable() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        SmtpOtpSender sender = new SmtpOtpSender(
                factory.getBeanProvider(JavaMailSender.class),
                "smtp.gmail.com", 587, "sender@gmail.com", "app-password", "sender@gmail.com", true);

        assertThatThrownBy(sender::ensureConfigured)
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void requiresGmailStartTlsConfiguration() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpOtpSender noTls = new SmtpOtpSender(
                provider(mailSender), "smtp.gmail.com", 587,
                "sender@gmail.com", "app-password", "sender@gmail.com", false);
        SmtpOtpSender insecureHost = new SmtpOtpSender(
                provider(mailSender), "mail.example.com", 587,
                "sender@gmail.com", "app-password", "sender@gmail.com", true);

        assertThatThrownBy(noTls::ensureConfigured).isInstanceOf(ApiException.class);
        assertThatThrownBy(insecureHost::ensureConfigured).isInstanceOf(ApiException.class);
    }

    private SmtpOtpSender configuredSender(JavaMailSender mailSender, boolean tls) {
        return new SmtpOtpSender(
                provider(mailSender), "smtp.gmail.com", 587,
                "sender@gmail.com", "app-password", "sender@gmail.com", tls);
    }

    private org.springframework.beans.factory.ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("mailSender", mailSender);
        return factory.getBeanProvider(JavaMailSender.class);
    }
}

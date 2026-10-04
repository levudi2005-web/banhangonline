package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.banhangonline.auth.service.SmtpOtpSender;
import com.banhangonline.common.exception.ApiException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@ExtendWith(OutputCaptureExtension.class)
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
    void reportsSanitizedSmtpCauseChain(CapturedOutput output) {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException(
                        "provider rejected sender@gmail.com app-password",
                        new IllegalStateException("nested sender@gmail.com app-password")))
                .when(mailSender).send(any(SimpleMailMessage.class));
        SmtpOtpSender sender = configuredSender(mailSender, true);

        assertThatThrownBy(() -> sender.send("customer@gmail.com", "042671", 300))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException apiError = (ApiException) error;
                    assertThat(apiError.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiError.getCode()).isEqualTo("EMAIL_DELIVERY_FAILED");
                    assertThat(apiError.getMessage()).doesNotContain("sender@gmail.com", "app-password");
                });
        assertThat(output.getOut())
                .contains("exceptionClass=org.springframework.mail.MailSendException")
                .contains("rootCauseClass=java.lang.IllegalStateException")
                .contains("nested [REDACTED] [REDACTED]")
                .doesNotContain("sender@gmail.com", "app-password", "customer@gmail.com", "042671");
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

    @Test
    void springMailReadsTheSmtpEnvironmentPlaceholdersAndSecuritySettings() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withUserConfiguration(SmtpSenderTestConfiguration.class)
                .withPropertyValues(
                        "SMTP_HOST=smtp.gmail.com",
                        "SMTP_PORT=587",
                        "SMTP_USERNAME=sender@gmail.com",
                        "SMTP_PASSWORD=unit-test-secret",
                        "SMTP_FROM=sender@gmail.com",
                        "SMTP_TLS_ENABLED=true",
                        "spring.mail.host=${SMTP_HOST:smtp.gmail.com}",
                        "spring.mail.port=${SMTP_PORT:587}",
                        "spring.mail.username=${SMTP_USERNAME:}",
                        "spring.mail.password=${SMTP_PASSWORD:}",
                        "spring.mail.properties.mail.smtp.auth=true",
                        "spring.mail.properties.mail.smtp.starttls.enable=${SMTP_TLS_ENABLED:true}",
                        "spring.mail.properties.mail.smtp.starttls.required=${SMTP_TLS_ENABLED:true}",
                        "spring.mail.properties.mail.smtp.ssl.checkserveridentity=true",
                        "spring.mail.properties.mail.smtp.connectiontimeout=10000",
                        "spring.mail.properties.mail.smtp.timeout=10000",
                        "spring.mail.properties.mail.smtp.writetimeout=10000")
                .run(context -> {
                    JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
                    Properties properties = mailSender.getJavaMailProperties();

                    assertThat(mailSender.getHost()).isEqualTo("smtp.gmail.com");
                    assertThat(mailSender.getPort()).isEqualTo(587);
                    assertThat(mailSender.getUsername()).isEqualTo("sender@gmail.com");
                    assertThat(mailSender.getPassword().equals("unit-test-secret")).isTrue();
                    assertThat(properties.getProperty("mail.smtp.auth")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.ssl.checkserveridentity")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("10000");
                    assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("10000");
                    assertThat(properties.getProperty("mail.smtp.writetimeout")).isEqualTo("10000");
                    context.getBean(SmtpOtpSender.class).ensureConfigured();
                });
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

    @Configuration(proxyBeanMethods = false)
    static class SmtpSenderTestConfiguration {
        @Bean
        SmtpOtpSender smtpOtpSender(
                ObjectProvider<JavaMailSender> mailSenders,
                @Value("${SMTP_HOST:smtp.gmail.com}") String host,
                @Value("${SMTP_PORT:587}") int port,
                @Value("${SMTP_USERNAME:}") String username,
                @Value("${SMTP_PASSWORD:}") String password,
                @Value("${SMTP_FROM:}") String from,
                @Value("${SMTP_TLS_ENABLED:true}") boolean tlsEnabled) {
            return new SmtpOtpSender(mailSenders, host, port, username, password, from, tlsEnabled);
        }
    }
}

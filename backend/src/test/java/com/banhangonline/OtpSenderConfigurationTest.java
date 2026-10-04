package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banhangonline.auth.service.SmtpOtpSender;
import com.banhangonline.auth.service.TwilioSmsOtpSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

class OtpSenderConfigurationTest {
    @Test
    void emailSenderFailsExplicitlyWhenProviderIsMissing() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        SmtpOtpSender sender = new SmtpOtpSender(
                beanFactory.getBeanProvider(JavaMailSender.class), "", "", "", "", true);

        assertThatThrownBy(sender::ensureConfigured)
                .isInstanceOf(com.banhangonline.common.exception.ApiException.class)
                .hasMessageContaining("Email OTP");
    }

    @Test
    void smsSenderFailsExplicitlyWhenProviderIsMissing() {
        TwilioSmsOtpSender sender = new TwilioSmsOtpSender(RestClient.builder(), "", "", "", "");

        assertThatThrownBy(sender::ensureConfigured)
                .isInstanceOf(com.banhangonline.common.exception.ApiException.class)
                .hasMessageContaining("SMS OTP");
    }
}

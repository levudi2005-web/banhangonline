package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banhangonline.auth.service.SmtpResetNotifier;
import com.banhangonline.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpResetNotifierTest {
    @Test
    void rejectsPasswordResetWhenSmtpIsNotConfigured() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        SmtpResetNotifier notifier = new SmtpResetNotifier(
                beanFactory.getBeanProvider(JavaMailSender.class), "", "", "", "", "", true);

        assertThatThrownBy(notifier::ensureConfigured)
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}

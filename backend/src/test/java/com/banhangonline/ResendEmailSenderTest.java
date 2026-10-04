package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.banhangonline.auth.service.OtpSender;
import com.banhangonline.auth.service.ResendEmailConfiguration;
import com.banhangonline.auth.service.ResendEmailSender;
import com.banhangonline.common.exception.ApiException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class ResendEmailSenderTest {
    private static final String API_BASE = "https://api.resend.com";
    private static final String API_PATH = API_BASE + "/emails";
    private static final String API_KEY = "test-resend-api-key";
    private static final String DESTINATION = "customer@example.com";
    private static final String OTP = "042671";

    @Test
    void sendsEmailThroughResendWithExpectedRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API_BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendEmailSender sender = sender(builder.build(), API_KEY, "no-reply@example.com", "Banhang Online");
        server.expect(requestTo(API_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "from": "Banhang Online <no-reply@example.com>",
                          "to": ["customer@example.com"],
                          "subject": "Mã xác minh",
                          "html": "<p>Mã xác minh của bạn là <strong>042671</strong>.</p><p>Mã hết hạn sau 5 phút. Không chia sẻ mã này với bất kỳ ai.</p>"
                        }
                        """))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("{\"id\":\"test-message-id\"}", MediaType.APPLICATION_JSON));

        sender.send(DESTINATION, OTP, 300);

        server.verify();
        assertThat(sender.channel().name()).isEqualTo("EMAIL");
    }

    @Test
    void mapsResendFourAndFiveHundredResponsesToDeliveryFailure() {
        for (HttpStatus status : List.of(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR)) {
            RestClient.Builder builder = RestClient.builder().baseUrl(API_BASE);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            ResendEmailSender sender = sender(builder.build(), API_KEY, "no-reply@example.com", "Banhang");
            server.expect(requestTo(API_PATH))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(status)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"message\":\"provider rejected request\"}"));

            assertDeliveryFailure(sender);
            server.verify();
        }
    }

    @Test
    void handlesNetworkFailureWithoutLoggingCredentialsOrEmailPayload(CapturedOutput output) {
        ClientHttpRequestFactory failingFactory = (URI uri, HttpMethod method) -> {
            throw new IOException("connection failed for " + API_KEY + " " + DESTINATION + " " + OTP);
        };
        RestClient client = RestClient.builder()
                .baseUrl(API_BASE)
                .requestFactory(failingFactory)
                .build();
        ResendEmailSender sender = sender(client, API_KEY, "no-reply@example.com", "Banhang");

        assertDeliveryFailure(sender);
        assertThat(output.getOut())
                .contains("category=network_or_timeout")
                .contains("rootCauseClass=java.io.IOException")
                .doesNotContain(API_KEY, DESTINATION, OTP, "no-reply@example.com", "Banhang");
    }

    @Test
    void handlesResendReadTimeout() {
        ClientHttpRequestFactory timeoutFactory = (URI uri, HttpMethod method) -> {
            throw new SocketTimeoutException("provider response timed out");
        };
        RestClient client = RestClient.builder()
                .baseUrl(API_BASE)
                .requestFactory(timeoutFactory)
                .build();
        ResendEmailSender sender = sender(client, API_KEY, "no-reply@example.com", "Banhang");

        assertDeliveryFailure(sender);
    }

    @Test
    void rejectsMissingApiKeyFromEmailAndFromName() {
        assertUnavailable(sender(RestClient.builder().build(), "", "no-reply@example.com", "Banhang"));
        assertUnavailable(sender(RestClient.builder().build(), API_KEY, "", "Banhang"));
        assertUnavailable(sender(senderClient(), API_KEY, "no-reply@example.com", ""));
    }

    @Test
    void rejectsMalformedOrIncompleteSuccessResponse() {
        for (String response : List.of("{}", "not-json")) {
            RestClient.Builder builder = RestClient.builder().baseUrl(API_BASE);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            ResendEmailSender sender = sender(builder.build(), API_KEY, "no-reply@example.com", "Banhang");
            server.expect(requestTo(API_PATH))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                            .withSuccess(response, MediaType.APPLICATION_JSON));

            assertDeliveryFailure(sender);
            server.verify();
        }
    }

    @Test
    void springConfigurationBindsResendEnvironmentVariablesAndFailsWhenMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withUserConfiguration(ResendEmailConfiguration.class, SenderConfiguration.class)
                .withPropertyValues(
                        "RESEND_API_KEY=" + API_KEY,
                        "RESEND_FROM_EMAIL=no-reply@example.com",
                        "RESEND_FROM_NAME=Banhang Online")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OtpSender sender = context.getBean(ResendEmailSender.class);
                    sender.ensureConfigured();
                });

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withUserConfiguration(ResendEmailConfiguration.class, SenderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertUnavailable(context.getBean(ResendEmailSender.class));
                });
    }

    private ResendEmailSender sender(RestClient client, String apiKey, String fromEmail, String fromName) {
        return new ResendEmailSender(client, apiKey, fromEmail, fromName);
    }

    private RestClient senderClient() {
        return RestClient.builder().build();
    }

    private void assertUnavailable(ResendEmailSender sender) {
        assertThatThrownBy(sender::ensureConfigured)
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException apiError = (ApiException) error;
                    assertThat(apiError.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiError.getCode()).isEqualTo("EMAIL_DELIVERY_UNAVAILABLE");
                });
    }

    private void assertDeliveryFailure(ResendEmailSender sender) {
        assertThatThrownBy(() -> sender.send(DESTINATION, OTP, 300))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException apiError = (ApiException) error;
                    assertThat(apiError.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiError.getCode()).isEqualTo("EMAIL_DELIVERY_FAILED");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SenderConfiguration {
        @Bean
        ResendEmailSender resendEmailSender(
                @Qualifier("resendRestClient") RestClient client,
                @Value("${RESEND_API_KEY:}") String apiKey,
                @Value("${RESEND_FROM_EMAIL:}") String fromEmail,
                @Value("${RESEND_FROM_NAME:}") String fromName) {
            return new ResendEmailSender(client, apiKey, fromEmail, fromName);
        }
    }
}

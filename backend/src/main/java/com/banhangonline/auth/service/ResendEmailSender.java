package com.banhangonline.auth.service;

import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.common.exception.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ResendEmailSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final String RESEND_EMAIL_PATH = "/emails";
    private final RestClient resend;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public ResendEmailSender(
            @Qualifier("resendRestClient") RestClient resend,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${RESEND_FROM_EMAIL:}") String fromEmail,
            @Value("${RESEND_FROM_NAME:}") String fromName) {
        this.resend = resend;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void ensureConfigured() {
        if (!StringUtils.hasText(apiKey)
                || !StringUtils.hasText(fromEmail)
                || !StringUtils.hasText(fromName)) {
            throw unavailable("EMAIL_DELIVERY_UNAVAILABLE", "Email OTP hiện chưa khả dụng.");
        }
    }

    @Override
    public void send(String destination, String code, int expirationSeconds) {
        ensureConfigured();
        String htmlContent = "<p>Mã xác minh của bạn là <strong>"
                + escapeHtml(code)
                + "</strong>.</p><p>Mã hết hạn sau "
                + Math.max(1, (expirationSeconds + 59) / 60)
                + " phút. Không chia sẻ mã này với bất kỳ ai.</p>";
        ResendEmailRequest request = new ResendEmailRequest(
                fromName + " <" + fromEmail + ">",
                List.of(destination),
                "Mã xác minh",
                htmlContent);

        try {
            ResendEmailResponse response = resend.post()
                    .uri(RESEND_EMAIL_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ResendEmailResponse.class);
            if (response == null || !StringUtils.hasText(response.id())) {
                log.warn("Resend transactional email failed: category=invalid_response");
                throw unavailable("EMAIL_DELIVERY_FAILED", "Không thể gửi mã xác minh qua email.");
            }
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String category = e.getStatusCode().is4xxClientError()
                    ? "provider_rejected_request"
                    : "provider_server_error";
            log.warn("Resend transactional email failed: status={}, category={}", status, category);
            throw unavailable("EMAIL_DELIVERY_FAILED", "Không thể gửi mã xác minh qua email.");
        } catch (RestClientException e) {
            String category = e instanceof ResourceAccessException
                    ? "network_or_timeout"
                    : "invalid_response_or_request";
            Throwable root = deepestCause(e);
            log.warn(
                    "Resend transactional email failed: exceptionClass={}, category={}, rootCauseClass={}, rootCauseMessage={}",
                    e.getClass().getName(),
                    category,
                    root.getClass().getName(),
                    e instanceof ResourceAccessException
                            ? redact(root.getMessage(), destination, code)
                            : "[omitted]");
            throw unavailable("EMAIL_DELIVERY_FAILED", "Không thể gửi mã xác minh qua email.");
        }
    }

    private Throwable deepestCause(Throwable error) {
        Throwable current = error;
        java.util.Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (current != null && visited.add(current)) {
            Throwable next = current.getCause();
            if (next == null || next == current) {
                return current;
            }
            current = next;
        }
        return current == null ? error : current;
    }

    private String redact(String message, String destination, String code) {
        String sanitized = String.valueOf(message);
        for (String sensitive : List.of(apiKey, fromEmail, fromName, destination, code)) {
            if (StringUtils.hasText(sensitive)) {
                sanitized = sanitized.replace(sensitive, "[REDACTED]");
            }
        }
        return sanitized;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private ApiException unavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {}

    private record ResendEmailResponse(String id) {}
}

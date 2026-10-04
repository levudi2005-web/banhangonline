package com.banhangonline.auth.service;

import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.common.exception.ApiException;
import java.net.http.HttpClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Component
public class TwilioSmsOtpSender implements OtpSender {
    private static final String TWILIO_API_BASE_URL = "https://api.twilio.com";
    private final RestClient restClient;
    private final String provider;
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioSmsOtpSender(
            RestClient.Builder builder,
            @Value("${app.sms.provider:}") String provider,
            @Value("${app.sms.account-sid:}") String accountSid,
            @Value("${app.sms.auth-token:}") String authToken,
            @Value("${app.sms.from:}") String from) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.requestFactory(requestFactory).build();
        this.provider = provider;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.from = from;
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.SMS;
    }

    @Override
    public void ensureConfigured() {
        if (!"TWILIO".equalsIgnoreCase(provider) || !StringUtils.hasText(accountSid)
                || !StringUtils.hasText(authToken) || !StringUtils.hasText(from)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_DELIVERY_UNAVAILABLE",
                    "Dịch vụ SMS OTP hiện chưa khả dụng.");
        }
        if (!accountSid.matches("AC[0-9a-fA-F]{32}") || !from.matches("\\+[1-9]\\d{7,14}")) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_DELIVERY_UNAVAILABLE",
                    "Dịch vụ SMS OTP hiện chưa khả dụng.");
        }
    }

    @Override
    public void send(String destination, String code, int expirationSeconds) {
        ensureConfigured();
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", "+84" + destination.substring(1));
        form.add("From", from);
        form.add("Body", "Mã xác minh: " + code + ". Hết hạn sau "
                + Math.max(1, (expirationSeconds + 59) / 60) + " phút. Không chia sẻ mã này.");
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        try {
            restClient.post()
                    .uri(URI.create(TWILIO_API_BASE_URL + "/2010-04-01/Accounts/"
                            + accountSid + "/Messages.json")
                    )
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "Basic " + credentials)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_DELIVERY_FAILED",
                    "Không thể gửi mã xác minh qua SMS.");
        }
    }
}

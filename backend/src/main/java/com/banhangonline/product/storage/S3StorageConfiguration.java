package com.banhangonline.product.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3StorageConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.storage.s3", name = "enabled", havingValue = "true")
    S3Client productImageS3Client(S3StorageProperties properties) {
        requireConfiguration(properties);
        var builder = S3Client.builder()
                .region(Region.of(properties.getRegion().trim()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.getAccessKey().trim(), properties.getSecretKey().trim())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(!properties.getEndpoint().isBlank())
                        .build());
        if (!properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint().trim()));
        }
        return builder.build();
    }

    private void requireConfiguration(S3StorageProperties properties) {
        if (isBlank(properties.getRegion()) || isBlank(properties.getBucket())
                || isBlank(properties.getAccessKey()) || isBlank(properties.getSecretKey())) {
            throw new IllegalStateException("S3_ENABLED=true requires S3_REGION, S3_BUCKET, "
                    + "S3_ACCESS_KEY, and S3_SECRET_KEY.");
        }
        URI publicBaseUrl;
        try {
            publicBaseUrl = URI.create(properties.getPublicBaseUrl().trim());
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("S3_PUBLIC_BASE_URL must be a valid HTTP(S) URL.", error);
        }
        if (!"https".equalsIgnoreCase(publicBaseUrl.getScheme())
                || publicBaseUrl.getHost() == null
                || publicBaseUrl.getQuery() != null
                || publicBaseUrl.getFragment() != null) {
            throw new IllegalStateException("S3_PUBLIC_BASE_URL must be an absolute HTTPS URL "
                    + "without a query or fragment.");
        }
        if (properties.getPublicBaseUrl().trim().replaceAll("/+$", "").length() > 420) {
            throw new IllegalStateException("S3_PUBLIC_BASE_URL is too long for product image URLs.");
        }
        if (!properties.getEndpoint().isBlank()) {
            URI endpoint;
            try {
                endpoint = URI.create(properties.getEndpoint().trim());
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException("S3_ENDPOINT must be a valid HTTP(S) URL.", error);
            }
            if (!("https".equalsIgnoreCase(endpoint.getScheme())
                    || "http".equalsIgnoreCase(endpoint.getScheme()))
                    || endpoint.getHost() == null) {
                throw new IllegalStateException("S3_ENDPOINT must be an absolute HTTP(S) URL.");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

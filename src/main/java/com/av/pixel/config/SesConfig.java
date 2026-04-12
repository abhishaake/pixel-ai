package com.av.pixel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@ConditionalOnProperty(name = "aws.ses.enabled", havingValue = "true")
public class SesConfig {

    @Value("${aws.ses.access-key-id}")
    private String accessKeyId;

    @Value("${aws.ses.secret-access-key}")
    private String secretAccessKey;

    @Value("${aws.ses.region}")
    private String region;

    @Bean(destroyMethod = "close")
    public SesClient sesClient() {
        validateCredentials();
        return SesClient.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())))
                .build();
    }

    @Bean(destroyMethod = "close")
    public SesAsyncClient sesAsyncClient() {
        validateCredentials();
        return SesAsyncClient.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())))
                .build();
    }

    private void validateCredentials() {
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalStateException(
                    "aws.ses.enabled is true but aws.ses.access-key-id (or AWS_ACCESS_KEY_ID env) is blank");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new IllegalStateException(
                    "aws.ses.enabled is true but aws.ses.secret-access-key (or AWS_SECRET_ACCESS_KEY env) is blank");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalStateException("aws.ses.region (or AWS_REGION env) is blank");
        }
    }
}

package com.odgiedev.reservvo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class AwsSesConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    @Bean
    public SesClient sesClient() {
        var builder = SesClient.builder().region(Region.of(region));

        if (accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            builder.credentialsProvider(AnonymousCredentialsProvider.create());
        } else {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            ));
        }

        return builder.build();
    }
}
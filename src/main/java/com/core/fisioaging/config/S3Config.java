package com.core.fisioaging.config;

import org.springframework.beans.factory.annotation.Value;import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static software.amazon.awssdk.regions.Region.*;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Bean
    public S3Client s3Client(@Value("${aws.s3.region}") String region) {
        return S3Client.builder()
                .region(of(region))
                .build();
    }
}

package com.shipmate.api.storage;

import com.shipmate.api.config.ShipmateStorageProperties;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Bean
    @Qualifier("internalMinioClient")
    MinioClient internalMinioClient(ShipmateStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    @Qualifier("publicMinioClient")
    MinioClient publicMinioClient(ShipmateStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.publicEndpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }
}

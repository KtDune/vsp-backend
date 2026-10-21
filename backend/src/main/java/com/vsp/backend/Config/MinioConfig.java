package com.vsp.backend.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
@EnableConfigurationProperties(MinioConfig.MinioProperties.class)
public class MinioConfig {

	@ConfigurationProperties(prefix = "minio")
	public record MinioProperties(
			String endpoint,
			String accessKey,
			String secretKey,
			String bucket,
			String publicUrl) {
	}

	@Bean
	public MinioClient minioClient(MinioProperties properties) {
		return MinioClient.builder()
				.endpoint(properties.endpoint())
				.credentials(properties.accessKey(), properties.secretKey())
				.build();
	}
}

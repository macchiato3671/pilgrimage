package com.ssafy.pilgrimage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AwsS3Config {

	private final PilgrimageProperties properties;

	@Bean
	@Lazy
	public S3Client s3Client() {
		return S3Client.builder()
				.region(region())
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}

	@Bean
	@Lazy
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
				.region(region())
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}

	private Region region() {
		String region = properties.getStorage().getRegion();
		if (region == null || region.isBlank()) {
			throw new IllegalStateException("storage.region(AWS_REGION) is not configured");
		}
		return Region.of(region);
	}
}

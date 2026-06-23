package com.moonback.pilgrimage.batch.ingest.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.moonback.pilgrimage.batch.ingest.config.PilgrimageProperties;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class S3ImageStorage {

	private final PilgrimageProperties properties;
	private S3Client s3Client;

	public boolean uploadIfMissing(String objectKey, Path webpFile, byte[] contentHash) throws IOException {
		if (exists(objectKey)) {
			return true;
		}
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket())
				.key(objectKey)
				.contentType("image/webp")
				.cacheControl("public,max-age=31536000,immutable")
				.checksumSHA256(Base64.getEncoder().encodeToString(contentHash))
				.build();
		client().putObject(request, RequestBody.fromFile(webpFile));
		return false;
	}

	public String urlFor(String objectKey) {
		String publicBaseUrl = properties.getStorage().getPublicBaseUrl();
		if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
			return publicBaseUrl.replaceAll("/+$", "") + "/" + objectKey;
		}
		return "s3://" + bucket() + "/" + objectKey;
	}

	private boolean exists(String objectKey) {
		try {
			client().headObject(HeadObjectRequest.builder().bucket(bucket()).key(objectKey).build());
			return true;
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return false;
			}
			throw e;
		}
	}

	private synchronized S3Client client() {
		if (s3Client == null) {
			String region = properties.getStorage().getRegion();
			if (region == null || region.isBlank()) {
				throw new ImageProcessingException("STORAGE_CONFIG_MISSING", "AWS_REGION is not configured");
			}
			s3Client = S3Client.builder()
					.region(Region.of(region))
					.credentialsProvider(DefaultCredentialsProvider.create())
					.build();
		}
		return s3Client;
	}

	private String bucket() {
		String bucket = properties.getStorage().getBucket();
		if (bucket == null || bucket.isBlank()) {
			throw new ImageProcessingException("STORAGE_CONFIG_MISSING", "S3_BUCKET is not configured");
		}
		return bucket;
	}
}

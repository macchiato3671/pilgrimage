package com.moonback.pilgrimage.model.service.impl;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.moonback.pilgrimage.model.service.S3PresignedUrlService;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlServiceImpl implements S3PresignedUrlService {

	private final PilgrimageProperties properties;
	private final ObjectProvider<S3Presigner> s3PresignerProvider;

	@Override
	public String toPresignedGetUrl(String storedUrl) {
		return extractS3Object(storedUrl)
				.map(s3Object -> createPresignedGetUrl(s3Object.bucket(), s3Object.objectKey()))
				.orElse(storedUrl);
	}

	@Override
	public String createPresignedGetUrl(String objectKey) {
		return createPresignedGetUrl(bucket(), objectKey);
	}

	@Override
	public String createPresignedGetUrl(String bucket, String objectKey) {
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalArgumentException("S3 bucket is blank");
		}

		String key = normalizeObjectKey(objectKey);
		if (key.isBlank()) {
			throw new IllegalArgumentException("S3 object key is blank");
		}

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();
		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(presignedUrlExpiration())
				.getObjectRequest(getObjectRequest)
				.build();

		return s3PresignerProvider.getObject().presignGetObject(presignRequest).url().toString();
	}

	private Optional<S3Object> extractS3Object(String storedUrl) {
		if (storedUrl == null || storedUrl.isBlank()) {
			return Optional.empty();
		}

		String url = storedUrl.trim();
		if (url.startsWith("s3://")) {
			return parseS3Uri(url);
		}

		Optional<String> publicBaseUrlKey = extractPublicBaseUrlKey(url);
		if (publicBaseUrlKey.isPresent()) {
			return Optional.of(new S3Object(bucket(), publicBaseUrlKey.get()));
		}

		Optional<S3Object> awsS3Url = parseAwsS3Url(url);
		if (awsS3Url.isPresent()) {
			return awsS3Url;
		}

		if (isAbsoluteHttpUrl(url)) {
			return Optional.empty();
		}

		String objectKey = normalizeObjectKey(url);
		return objectKey.isBlank()
				? Optional.empty()
				: Optional.of(new S3Object(bucket(), objectKey));
	}

	private Optional<S3Object> parseS3Uri(String url) {
		try {
			URI uri = URI.create(url);
			String bucket = uri.getHost();
			String objectKey = normalizeObjectKey(uri.getPath());
			if (bucket == null || bucket.isBlank() || objectKey.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(new S3Object(bucket, objectKey));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private Optional<String> extractPublicBaseUrlKey(String url) {
		String publicBaseUrl = properties.getStorage().getPublicBaseUrl();
		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			return Optional.empty();
		}

		String baseUrl = trimTrailingSlash(publicBaseUrl.trim());
		if (!url.startsWith(baseUrl + "/")) {
			return Optional.empty();
		}

		String objectKey = normalizeObjectKey(url.substring(baseUrl.length() + 1));
		return objectKey.isBlank() ? Optional.empty() : Optional.of(objectKey);
	}

	private Optional<S3Object> parseAwsS3Url(String url) {
		try {
			URI uri = URI.create(url);
			String host = uri.getHost();
			if (host == null) {
				return Optional.empty();
			}

			String objectKey = normalizeObjectKey(uri.getPath());
			if (objectKey.isBlank()) {
				return Optional.empty();
			}

			int virtualHostedS3Index = host.indexOf(".s3");
			if (virtualHostedS3Index > 0) {
				return Optional.of(new S3Object(host.substring(0, virtualHostedS3Index), objectKey));
			}

			if (host.equals("s3.amazonaws.com") || host.startsWith("s3.")) {
				int firstSlash = objectKey.indexOf('/');
				if (firstSlash <= 0 || firstSlash == objectKey.length() - 1) {
					return Optional.empty();
				}
				return Optional.of(new S3Object(objectKey.substring(0, firstSlash), objectKey.substring(firstSlash + 1)));
			}

			return Optional.empty();
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private boolean isAbsoluteHttpUrl(String url) {
		return url.startsWith("http://") || url.startsWith("https://");
	}

	private String normalizeObjectKey(String objectKey) {
		if (objectKey == null) {
			return "";
		}
		return objectKey.replaceFirst("^/+", "");
	}

	private String trimTrailingSlash(String value) {
		return value.replaceFirst("/+$", "");
	}

	private String bucket() {
		String bucket = properties.getStorage().getBucket();
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalStateException("storage.bucket(S3_BUCKET) is not configured");
		}
		return bucket;
	}

	private Duration presignedUrlExpiration() {
		Duration expiration = properties.getStorage().getPresignedUrlExpiration();
		if (expiration == null || expiration.isZero() || expiration.isNegative()) {
			throw new IllegalStateException("storage.presigned-url-expiration must be positive");
		}
		return expiration;
	}

	private record S3Object(String bucket, String objectKey) {
	}
}

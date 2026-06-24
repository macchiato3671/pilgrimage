package com.moonback.pilgrimage.model.service;

public interface S3PresignedUrlService {

	String toPresignedGetUrl(String storedUrl);

	String createPresignedGetUrl(String objectKey);

	String createPresignedGetUrl(String bucket, String objectKey);
}

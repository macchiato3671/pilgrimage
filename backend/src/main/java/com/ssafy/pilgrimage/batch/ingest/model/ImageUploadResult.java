package com.ssafy.pilgrimage.batch.ingest.model;

import lombok.Builder;

@Builder
public record ImageUploadResult(
		byte[] taskKey,
		String objectKey,
		String url,
		byte[] contentHash,
		int width,
		int height,
		boolean reusedExistingObject) {
}

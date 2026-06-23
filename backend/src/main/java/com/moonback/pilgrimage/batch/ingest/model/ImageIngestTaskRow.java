package com.moonback.pilgrimage.batch.ingest.model;

import lombok.Builder;

@Builder
public record ImageIngestTaskRow(
		byte[] taskKey,
		OwnerType ownerType,
		int ownerId,
		ImageType imageType,
		String sourceUrl,
		String sourceIdentity,
		int sortOrder,
		String objectKey,
		byte[] contentHash,
		Integer width,
		Integer height,
		IngestStatus status,
		int attemptCount,
		String errorCode,
		String errorMessage) {
}

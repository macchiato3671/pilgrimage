package com.ssafy.pilgrimage.batch.ingest.model;

import lombok.Builder;

@Builder
public record ImageTaskUpsert(
		byte[] taskKey,
		OwnerType ownerType,
		int ownerId,
		ImageType imageType,
		String sourceUrl,
		String sourceIdentity,
		int sortOrder) {
}

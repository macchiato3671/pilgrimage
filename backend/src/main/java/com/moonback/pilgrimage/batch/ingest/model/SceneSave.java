package com.moonback.pilgrimage.batch.ingest.model;

import lombok.Builder;

@Builder
public record SceneSave(
		byte[] sourceKey,
		int dramaId,
		String name,
		String description,
		String address,
		double latitude,
		double longitude,
		byte[] ingestKey,
		Integer existingSceneId) {
}

package com.moonback.pilgrimage.batch.ingest.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ParsedScene(
		byte[] sourceKey,
		int sourceOrder,
		int sameNameOccurrence,
		String rawName,
		String normalizedName,
		String rawText,
		String rawAddress,
		List<String> imageUrls) {
}

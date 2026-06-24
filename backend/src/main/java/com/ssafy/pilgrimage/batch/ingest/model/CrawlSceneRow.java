package com.ssafy.pilgrimage.batch.ingest.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CrawlSceneRow(
		byte[] sourceKey,
		byte[] postKey,
		String postUrl,
		Integer tmdbId,
		int sourceOrder,
		int sameNameOccurrence,
		String rawName,
		String rawText,
		String rawAddress,
		List<String> imageUrls,
		Integer sceneId,
		IngestStatus status,
		String errorCode,
		String errorMessage) {
}

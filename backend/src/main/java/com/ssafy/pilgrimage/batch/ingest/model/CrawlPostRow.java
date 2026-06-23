package com.ssafy.pilgrimage.batch.ingest.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CrawlPostRow(
		byte[] postKey,
		String postUrl,
		String postTitle,
		LocalDateTime publishedAt,
		String dramaQuery,
		String normalizedQuery,
		Integer tmdbId,
		byte[] contentHash,
		IngestStatus status,
		String errorCode,
		String errorMessage) {
}

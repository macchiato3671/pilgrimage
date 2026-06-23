package com.moonback.pilgrimage.batch.ingest.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ParsedPost(
		byte[] postKey,
		String canonicalPostUrl,
		String dramaQuery,
		String normalizedQuery,
		byte[] contentHash,
		List<ParsedScene> scenes) {
}

package com.moonback.pilgrimage.batch.ingest.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record DramaUpsert(
		int dramaId,
		String title,
		String originalTitle,
		String originalLanguage,
		String originCountry,
		LocalDate releasedAt,
		String description,
		List<GenreUpsert> genres) {
}

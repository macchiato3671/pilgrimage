package com.moonback.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record TmdbImageResponse(
		List<TmdbImageDto> posters,
		List<TmdbImageDto> backdrops,
		List<TmdbImageDto> logos) {
}

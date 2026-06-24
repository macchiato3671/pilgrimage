package com.moonback.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record TmdbAlternativeTitle(
		@JsonProperty("iso_3166_1") String country,
		String title,
		String type) {
}

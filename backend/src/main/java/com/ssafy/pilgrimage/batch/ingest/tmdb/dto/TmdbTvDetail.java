package com.ssafy.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record TmdbTvDetail(
		int id,
		String name,
		@JsonProperty("original_name") String originalName,
		@JsonProperty("original_language") String originalLanguage,
		@JsonProperty("origin_country") List<String> originCountry,
		@JsonProperty("first_air_date") String firstAirDate,
		String overview,
		List<TmdbGenreDto> genres) {
}

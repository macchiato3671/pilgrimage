package com.ssafy.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record TmdbImageDto(
		@JsonProperty("file_path") String filePath,
		@JsonProperty("iso_639_1") String language,
		int width,
		int height,
		@JsonProperty("vote_average") double voteAverage,
		@JsonProperty("vote_count") int voteCount) {
}

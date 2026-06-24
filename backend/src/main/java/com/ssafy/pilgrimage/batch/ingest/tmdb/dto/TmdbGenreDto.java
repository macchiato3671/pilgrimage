package com.ssafy.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

@Builder
public record TmdbGenreDto(int id, String name) {
}

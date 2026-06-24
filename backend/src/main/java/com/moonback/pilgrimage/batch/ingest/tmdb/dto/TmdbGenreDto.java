package com.moonback.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

@Builder
public record TmdbGenreDto(int id, String name) {
}

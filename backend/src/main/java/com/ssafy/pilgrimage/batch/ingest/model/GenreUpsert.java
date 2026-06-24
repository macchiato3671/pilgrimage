package com.ssafy.pilgrimage.batch.ingest.model;

import lombok.Builder;

@Builder
public record GenreUpsert(int genreId, String name) {
}

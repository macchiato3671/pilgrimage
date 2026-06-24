package com.ssafy.pilgrimage.batch.ingest.tmdb.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record TmdbSearchResponse(List<TmdbTvSearchResult> results) {
}

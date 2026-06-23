package com.moonback.pilgrimage.batch.ingest.geocode.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record KakaoSearchResponse(List<KakaoDocument> documents) {
}

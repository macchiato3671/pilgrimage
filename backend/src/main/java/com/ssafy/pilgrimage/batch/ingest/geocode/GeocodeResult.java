package com.ssafy.pilgrimage.batch.ingest.geocode;

import lombok.Builder;

@Builder
public record GeocodeResult(
		String canonicalAddress,
		String region1Depth,
		String region2Depth,
		double latitude,
		double longitude,
		String responseJson) {
}

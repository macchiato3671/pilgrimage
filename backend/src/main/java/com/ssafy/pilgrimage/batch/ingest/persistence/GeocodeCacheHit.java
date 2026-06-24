package com.ssafy.pilgrimage.batch.ingest.persistence;

import lombok.Builder;

import com.ssafy.pilgrimage.batch.ingest.geocode.GeocodeResult;

@Builder
public record GeocodeCacheHit(String status, String queryText, String canonicalAddress, String region1Depth,
		String region2Depth, Double latitude, Double longitude, String responseJson) {

	public boolean success() {
		return "SUCCESS".equals(status);
	}

	public GeocodeResult toResult() {
		if (!success() || latitude == null || longitude == null) {
			return null;
		}
		return new GeocodeResult(canonicalAddress, region1Depth, region2Depth, latitude, longitude, responseJson);
	}
}

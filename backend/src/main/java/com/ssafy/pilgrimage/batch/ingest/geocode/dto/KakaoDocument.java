package com.ssafy.pilgrimage.batch.ingest.geocode.dto;

import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record KakaoDocument(
		@JsonProperty("place_name") String placeName,
		@JsonProperty("address_name") String addressName,
		@JsonProperty("road_address_name") String roadAddressName,
		String x,
		String y,
		KakaoAddress address,
		@JsonProperty("road_address") KakaoRoadAddress roadAddress) {

	public String canonicalAddress() {
		if (roadAddressName != null && !roadAddressName.isBlank()) {
			return roadAddressName;
		}
		if (roadAddress != null && roadAddress.addressName() != null && !roadAddress.addressName().isBlank()) {
			return roadAddress.addressName();
		}
		if (addressName != null && !addressName.isBlank()) {
			return addressName;
		}
		if (address != null) {
			return address.addressName();
		}
		return null;
	}

	public String region1Depth() {
		if (address != null && address.region1DepthName() != null && !address.region1DepthName().isBlank()) {
			return address.region1DepthName();
		}
		String canonical = canonicalAddress();
		if (canonical == null || canonical.isBlank()) {
			return null;
		}
		return canonical.split("\\s+")[0];
	}

	public String region2Depth() {
		if (address != null && address.region2DepthName() != null && !address.region2DepthName().isBlank()) {
			return address.region2DepthName();
		}
		String canonical = canonicalAddress();
		if (canonical == null || canonical.isBlank()) {
			return null;
		}
		String[] parts = canonical.split("\\s+");
		return parts.length > 1 ? parts[1] : null;
	}
}

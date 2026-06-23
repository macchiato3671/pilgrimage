package com.ssafy.pilgrimage.batch.ingest.geocode.dto;

import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record KakaoAddress(
		@JsonProperty("address_name") String addressName,
		@JsonProperty("region_1depth_name") String region1DepthName,
		@JsonProperty("region_2depth_name") String region2DepthName) {
}

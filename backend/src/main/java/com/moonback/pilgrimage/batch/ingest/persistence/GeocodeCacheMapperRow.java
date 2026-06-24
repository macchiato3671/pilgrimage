package com.moonback.pilgrimage.batch.ingest.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeocodeCacheMapperRow {

	private String status;
	private String queryText;
	private String canonicalAddress;
	private String region1Depth;
	private String region2Depth;
	private Double latitude;
	private Double longitude;
	private String responseJson;
}

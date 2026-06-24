package com.ssafy.pilgrimage.batch.ingest.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrawlSceneMapperRow {

	private byte[] sourceKey;
	private byte[] postKey;
	private String postUrl;
	private Integer tmdbId;
	private int sourceOrder;
	private int sameNameOccurrence;
	private String rawName;
	private String rawText;
	private String rawAddress;
	private String imageUrls;
	private Integer sceneId;
	private String status;
	private String errorCode;
	private String errorMessage;
}

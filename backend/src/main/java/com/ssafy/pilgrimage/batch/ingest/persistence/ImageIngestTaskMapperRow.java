package com.ssafy.pilgrimage.batch.ingest.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageIngestTaskMapperRow {

	private byte[] taskKey;
	private String ownerType;
	private int ownerId;
	private String imageType;
	private String sourceUrl;
	private String sourceIdentity;
	private int sortOrder;
	private String objectKey;
	private byte[] contentHash;
	private Integer width;
	private Integer height;
	private String status;
	private int attemptCount;
	private String errorCode;
	private String errorMessage;
}

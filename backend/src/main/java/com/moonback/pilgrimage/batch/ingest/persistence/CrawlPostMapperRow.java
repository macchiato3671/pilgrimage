package com.moonback.pilgrimage.batch.ingest.persistence;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrawlPostMapperRow {

	private byte[] postKey;
	private String postUrl;
	private String postTitle;
	private LocalDateTime publishedAt;
	private String dramaQuery;
	private String normalizedQuery;
	private Integer tmdbId;
	private byte[] contentHash;
	private String status;
	private String errorCode;
	private String errorMessage;
}

package com.moonback.pilgrimage.batch.ingest.model;

import lombok.Builder;

@Builder
public record DiscoveredPost(byte[] postKey, String postUrl, String postTitle) {
}

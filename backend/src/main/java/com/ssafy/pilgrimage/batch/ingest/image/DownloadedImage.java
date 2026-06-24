package com.ssafy.pilgrimage.batch.ingest.image;

import lombok.Builder;

import java.nio.file.Path;

@Builder
public record DownloadedImage(Path path, String contentType, long bytes) {
}

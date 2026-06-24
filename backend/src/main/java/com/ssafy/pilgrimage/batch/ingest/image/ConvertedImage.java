package com.ssafy.pilgrimage.batch.ingest.image;

import lombok.Builder;

import java.nio.file.Path;

@Builder
public record ConvertedImage(Path path, int width, int height) {
}

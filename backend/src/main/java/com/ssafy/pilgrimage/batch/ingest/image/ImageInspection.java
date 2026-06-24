package com.ssafy.pilgrimage.batch.ingest.image;

import lombok.Builder;

@Builder
public record ImageInspection(ImageFormat format, int width, int height) {
	public long pixels() {
		return (long) width * (long) height;
	}
}

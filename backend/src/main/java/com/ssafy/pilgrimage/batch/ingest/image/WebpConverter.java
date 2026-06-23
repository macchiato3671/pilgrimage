package com.ssafy.pilgrimage.batch.ingest.image;

import java.nio.file.Path;

import com.ssafy.pilgrimage.batch.ingest.model.ImageType;

public interface WebpConverter {
	ConvertedImage convert(Path input, ImageInspection inspection, ImageType imageType);
}

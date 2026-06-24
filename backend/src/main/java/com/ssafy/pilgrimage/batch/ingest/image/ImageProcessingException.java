package com.ssafy.pilgrimage.batch.ingest.image;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ImageProcessingException extends RuntimeException {

	private final String errorCode;

	public ImageProcessingException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ImageProcessingException(String errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

}

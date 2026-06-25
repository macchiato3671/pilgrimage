package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SceneErrorCode implements ErrorCode {
	SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "Scene does not exists."),
	INVALID_RADIUS_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid radius parameter."),
	INVALID_PAGE_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid page parameter."),
	INVALID_CONTENT_TYPE_ID(HttpStatus.BAD_REQUEST, "Invalid content type ID.");
	
	private final HttpStatus status;
    private final String message;
}

package com.ssafy.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DramaErrorCode implements ErrorCode {
	DRAMA_NOT_FOUND(HttpStatus.NOT_FOUND, "Drama does not exists"), 
	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "Invalid page request.");
	
	private final HttpStatus status;
    private final String message;
}

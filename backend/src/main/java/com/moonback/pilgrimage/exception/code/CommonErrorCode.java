package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// 공통 에러 코드
public enum CommonErrorCode implements ErrorCode {
	
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed.");

    private final HttpStatus status;
    private final String message;
}

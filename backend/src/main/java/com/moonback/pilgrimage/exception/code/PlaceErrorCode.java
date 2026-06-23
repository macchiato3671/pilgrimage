package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
	// 관광지 상세 조회
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Place does not exists.");
	
    private final HttpStatus status;
    private final String message;
}

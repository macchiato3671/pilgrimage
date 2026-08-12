package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
	// 관광지 상세 조회
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Place does not exists."),

	// 관광지 조건 검색
	REQUIRED_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "At least one search condition is required."),
	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "Invalid page request."),
	INVALID_RADIUS_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid radius parameter.");

    private final HttpStatus status;
    private final String message;
}

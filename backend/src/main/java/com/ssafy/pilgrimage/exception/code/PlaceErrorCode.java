package com.ssafy.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
	// 관광�? ?�세 조회
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Place does not exists."),

	// 관광�? 조건 검??
	REQUIRED_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "At least one search condition is required."),
	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "Invalid page request."),
	INVALID_RADIUS_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid radius parameter.");

    private final HttpStatus status;
    private final String message;
}

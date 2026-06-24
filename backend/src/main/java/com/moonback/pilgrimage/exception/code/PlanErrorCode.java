package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanErrorCode implements ErrorCode {
	// API-010: 여행 일정 리스트 조회
	INVALID_PAGE_ARG(
			HttpStatus.BAD_REQUEST,
			"Page argument must be bigger than or equal to 1."
			),
	INVALID_PAGE_SIZE_ARG(
			HttpStatus.BAD_REQUEST,
			"Page size argument  must in the range of [1, 50]."
			),

	// API-009: 여행 일정 만들기
	INVALID_TRAVEL_PLAN_TITLE(
			HttpStatus.BAD_REQUEST,
			"Invalid travel plan detail target"
			),
	INVALID_TRAVEL_PLAN_DATE(
			HttpStatus.BAD_REQUEST,
			"Invalid travel plan date range"
			),
	REQUIRED_FIELD_MISSING(
			HttpStatus.BAD_REQUEST,
			"Required field is missing"
			),

	// API-014: travel plan update
	INVALID_PLAN_ID(
			HttpStatus.BAD_REQUEST,
			"Invalid travel plan ID."
			),
	TRAVEL_PLAN_ACCESS_DENIED(
			HttpStatus.FORBIDDEN,
			"Access to the travel plan is denied."
			),

	// API-011: travel plan detail lookup
	TRAVEL_PLAN_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"Travel plan not found"
			);

	private final HttpStatus status;
	private final String message;
}

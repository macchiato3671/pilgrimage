package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanErrorCode implements ErrorCode {
	INVALID_PAGE_ARG(
			HttpStatus.BAD_REQUEST,
			"Page argument must be bigger than or equal to 1."
			),
	INVALID_PAGE_SIZE_ARG(
			HttpStatus.BAD_REQUEST,
			"Page size argument  must in the range of [1, 50]."
			);

	private final HttpStatus status;
    private final String message;
}

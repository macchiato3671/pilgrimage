package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
	// 회원 가입
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "Invalid email format."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Invalid password format."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "Invalid nickname format."),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "Required field is missing.");

    private final HttpStatus status;
    private final String message;
}

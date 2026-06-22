package com.ssafy.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
	// 로그인
	INVALID_EMAIL(HttpStatus.BAD_REQUEST, "Invalid email format."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Invalid password format."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
    MEMBER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Member access is denied."),
	
	// JWT token
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token.");

    private final HttpStatus status;
    private final String message;
}

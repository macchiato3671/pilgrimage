package com.ssafy.pilgrimage.exception.code;

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
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "Required field is missing."),
	
	// 회원 정보 조회
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member not found."),
	MEMBER_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "Member already withdrawn."),
	MEMBER_SUSPENDED(HttpStatus.CONFLICT, "Member suspended."),
	MEMBER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Member access denied."),
	
	// 회원 정보 수정
	EMPTY_UPDATE_FILEDS(HttpStatus.BAD_REQUEST, "At least one field must be provided for update");
	
	
    private final HttpStatus status;
    private final String message;
}

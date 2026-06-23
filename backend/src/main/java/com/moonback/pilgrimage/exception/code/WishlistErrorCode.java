package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WishlistErrorCode implements ErrorCode{
	// 위시리스트에 씬 삽입
	WISHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "Scene already exists."),
	
	// 위시리스트 드라마 조회
	
	// 위시리스트 씬 조회
	INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "Invalid page request."),
	
	// 위시리스트 삭제
	WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "Wishlist Scene does not exists");
	
	private final HttpStatus status;
    private final String message;
}

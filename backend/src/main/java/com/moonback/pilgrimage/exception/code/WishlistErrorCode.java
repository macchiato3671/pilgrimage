package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WishlistErrorCode implements ErrorCode{
	// 위시리스트에 씬 삽입
	WISHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "Scene already exists.");
	
	private final HttpStatus status;
    private final String message;
}

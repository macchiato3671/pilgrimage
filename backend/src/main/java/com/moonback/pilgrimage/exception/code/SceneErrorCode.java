package com.moonback.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SceneErrorCode implements ErrorCode {
	SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "Scene does not exists.");
	
	private final HttpStatus status;
    private final String message;
}

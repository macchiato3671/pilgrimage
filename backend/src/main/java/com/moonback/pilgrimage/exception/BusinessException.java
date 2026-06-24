package com.moonback.pilgrimage.exception;

import com.moonback.pilgrimage.exception.code.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
	
	private final ErrorCode errorCode;
	
	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}

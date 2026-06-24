package com.moonback.pilgrimage.model.dto.response;

import com.moonback.pilgrimage.exception.code.ErrorCode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponseDto {
	private int status;
	private String errorCode;
	private String message;
	
	public static ErrorResponseDto from(ErrorCode errorCode) {
		return ErrorResponseDto.builder()
				.status(errorCode.getStatus().value())
				.errorCode(errorCode.name())
				.message(errorCode.getMessage())
				.build();
	}
}

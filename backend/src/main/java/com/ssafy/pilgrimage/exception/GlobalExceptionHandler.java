package com.ssafy.pilgrimage.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.pilgrimage.exception.code.CommonErrorCode;
import com.ssafy.pilgrimage.exception.code.ErrorCode;
import com.ssafy.pilgrimage.model.dto.response.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponseDto.from(errorCode));
	}
	
	@ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponseDto.from(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}

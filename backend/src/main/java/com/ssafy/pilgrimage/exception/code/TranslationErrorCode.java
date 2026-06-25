package com.ssafy.pilgrimage.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TranslationErrorCode implements ErrorCode {

    GMS_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "GMS_KEY is not configured."),
    INVALID_TRANSLATION_REQUEST(HttpStatus.BAD_REQUEST, "Invalid translation request."),
    TRANSLATION_FAILED(HttpStatus.BAD_GATEWAY, "Translation request failed.");

    private final HttpStatus status;
    private final String message;
}

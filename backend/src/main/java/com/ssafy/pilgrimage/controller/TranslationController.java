package com.ssafy.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pilgrimage.model.dto.request.TranslationRequestDto;
import com.ssafy.pilgrimage.model.dto.response.TranslationResponseDto;
import com.ssafy.pilgrimage.model.service.TranslationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    @PostMapping
    public ResponseEntity<TranslationResponseDto> translate(@Valid @RequestBody TranslationRequestDto request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(translationService.translate(request));
    }
}

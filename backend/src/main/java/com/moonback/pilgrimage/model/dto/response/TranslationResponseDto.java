package com.moonback.pilgrimage.model.dto.response;

import java.util.List;

public record TranslationResponseDto(
        String targetLanguage,
        List<String> translations
) {
}

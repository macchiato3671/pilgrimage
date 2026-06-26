package com.moonback.pilgrimage.model.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record TranslationRequestDto(
        @NotBlank
        @Size(max = 80)
        String targetLanguage,

        @NotEmpty
        @Size(max = 60)
        List<@NotBlank @Size(max = 900) String> texts
) {
}

package com.ssafy.pilgrimage.model.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.TranslationErrorCode;
import com.ssafy.pilgrimage.model.dto.request.TranslationRequestDto;
import com.ssafy.pilgrimage.model.dto.response.TranslationResponseDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TranslationService {

    private static final String DEFAULT_COMPLETIONS_URL =
            "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions";
    private static final String SYSTEM_PROMPT = """
            You translate application UI text.
            Return only strict JSON in this shape: {"translations":["..."]}.
            The translations array must have exactly the same length and order as the input texts array.
            Preserve brand names, product names, URLs, numbers, emoji, placeholders, and code-like tokens.
            Do not add explanations, markdown, numbering, or extra fields.
            """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final URI completionsUri;
    private final String model;

    public TranslationService(
            ObjectMapper objectMapper,
            @Value("${gms.key:}") String apiKey,
            @Value("${gms.chat-completions-url:" + DEFAULT_COMPLETIONS_URL + "}") String completionsUrl,
            @Value("${gms.model:gpt-4o-mini}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = normalizeConfigValue(apiKey);
        this.completionsUri = URI.create(normalizeConfigValue(completionsUrl));
        this.model = normalizeConfigValue(model);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public TranslationResponseDto translate(TranslationRequestDto request) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(TranslationErrorCode.GMS_KEY_MISSING);
        }

        List<String> texts = request.texts().stream()
                .map(String::trim)
                .toList();

        if (texts.stream().anyMatch(text -> !StringUtils.hasText(text))) {
            throw new BusinessException(TranslationErrorCode.INVALID_TRANSLATION_REQUEST);
        }

        return new TranslationResponseDto(request.targetLanguage(), requestTranslations(request.targetLanguage(), texts));
    }

    private List<String> requestTranslations(String targetLanguage, List<String> texts) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "developer", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", objectMapper.writeValueAsString(Map.of(
                                    "target_language", targetLanguage,
                                    "texts", texts
                            )))
                    )
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder(completionsUri)
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("GMS translation request failed. status={}, body={}", response.statusCode(), truncate(response.body()));
                throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
            }

            String content = objectMapper.readTree(response.body())
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (!StringUtils.hasText(content)) {
                log.warn("GMS translation response did not include choices[0].message.content. body={}", truncate(response.body()));
                throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
            }

            return parseTranslations(content, texts.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("GMS translation request was interrupted.", e);
            throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("GMS translation request could not be completed.", e);
            throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
        }
    }

    private List<String> parseTranslations(String content, int expectedSize) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(extractJson(content));
        JsonNode translationsNode = root.isArray() ? root : root.path("translations");

        if (!translationsNode.isArray() || translationsNode.size() != expectedSize) {
            log.warn(
                    "GMS translation response shape was invalid. expectedSize={}, content={}",
                    expectedSize,
                    truncate(content)
            );
            throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
        }

        List<String> translations = new ArrayList<>(expectedSize);
        translationsNode.forEach(node -> translations.add(node.asText()));
        return translations;
    }

    private String extractJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start = objectStart < 0 ? arrayStart
                : arrayStart < 0 ? objectStart
                : Math.min(objectStart, arrayStart);

        if (start < 0) {
            throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
        }

        char open = trimmed.charAt(start);
        char close = open == '{' ? '}' : ']';
        int end = trimmed.lastIndexOf(close);

        if (end < start) {
            throw new BusinessException(TranslationErrorCode.TRANSLATION_FAILED);
        }

        return trimmed.substring(start, end + 1);
    }

    private String normalizeConfigValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    private String truncate(String value) {
        if (value == null) return "";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 700 ? compact : compact.substring(0, 700) + "...";
    }
}

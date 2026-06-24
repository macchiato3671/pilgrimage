package com.ssafy.pilgrimage.batch.ingest.geocode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.geocode.dto.KakaoSearchResponse;
import com.ssafy.pilgrimage.batch.ingest.support.NonRetryableRemoteException;
import com.ssafy.pilgrimage.batch.ingest.support.RetrySupport;
import com.ssafy.pilgrimage.batch.ingest.support.RetryableRemoteException;
import com.ssafy.pilgrimage.batch.ingest.support.SimpleRateLimiter;

@Component
public class KakaoLocalClient {

	private final PilgrimageProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final SimpleRateLimiter rateLimiter;

	public KakaoLocalClient(PilgrimageProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		this.rateLimiter = new SimpleRateLimiter(properties.getKakao().getRequestPerSecond());
	}

	public KakaoSearchResponse addressSearch(String query, String analyzeType) {
		return get("/v2/local/search/address.json?query=" + encode(query) + "&analyze_type=" + encode(analyzeType));
	}

	public KakaoSearchResponse keywordSearch(String query) {
		return get("/v2/local/search/keyword.json?query=" + encode(query));
	}

	private KakaoSearchResponse get(String path) {
		return RetrySupport.withDefaultRetry(() -> getOnce(path));
	}

	private KakaoSearchResponse getOnce(String path) {
		if (properties.getKakao().getRestApiKey() == null || properties.getKakao().getRestApiKey().isBlank()) {
			throw new NonRetryableRemoteException("KAKAO_REST_API_KEY is not configured");
		}
		rateLimiter.acquire();
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getKakao().getBaseUrl()) + path))
					.timeout(Duration.ofSeconds(30))
					.header("Authorization", "KakaoAK " + properties.getKakao().getRestApiKey())
					.header("Accept", "application/json")
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status == 200) {
				return objectMapper.readValue(response.body(), KakaoSearchResponse.class);
			}
			if (status == 429) {
				throw new RetryableRemoteException("Kakao returned HTTP 429", retryAfter(response));
			}
			if (status >= 500 && status <= 504) {
				throw new RetryableRemoteException("Kakao returned HTTP " + status);
			}
			throw new NonRetryableRemoteException("Kakao returned HTTP " + status);
		} catch (IOException e) {
			throw new RetryableRemoteException("Kakao request failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RetryableRemoteException("Kakao request interrupted", e);
		}
	}

	private Duration retryAfter(HttpResponse<?> response) {
		return response.headers().firstValue("Retry-After")
				.map(value -> {
					try {
						return Duration.ofSeconds(Long.parseLong(value));
					} catch (NumberFormatException ignored) {
						return Duration.between(ZonedDateTime.now(),
								ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
					}
				})
				.orElse(Duration.ofSeconds(5));
	}

	private String encode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private String trimTrailingSlash(String value) {
		return value == null ? "" : value.replaceAll("/+$", "");
	}
}

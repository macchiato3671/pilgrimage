package com.ssafy.pilgrimage.batch.ingest.tmdb;

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
import com.ssafy.pilgrimage.batch.ingest.support.NonRetryableRemoteException;
import com.ssafy.pilgrimage.batch.ingest.support.RetrySupport;
import com.ssafy.pilgrimage.batch.ingest.support.RetryableRemoteException;
import com.ssafy.pilgrimage.batch.ingest.support.SimpleRateLimiter;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbAlternativeTitlesResponse;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbImageResponse;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbSearchResponse;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbTvDetail;

@Component
public class TmdbClient {

	private final PilgrimageProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final SimpleRateLimiter rateLimiter;

	public TmdbClient(PilgrimageProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		this.rateLimiter = new SimpleRateLimiter(properties.getTmdb().getRequestPerSecond());
	}

	public TmdbSearchResponse searchTv(String query) {
		String path = "/search/tv?query=" + encode(query) + "&language=" + encode(properties.getTmdb().getLanguage())
				+ "&include_adult=false";
		return get(path, TmdbSearchResponse.class);
	}

	public TmdbTvDetail detail(int tvId, String language) {
		return get("/tv/" + tvId + "?language=" + encode(language), TmdbTvDetail.class);
	}

	public TmdbAlternativeTitlesResponse alternativeTitles(int tvId) {
		return get("/tv/" + tvId + "/alternative_titles", TmdbAlternativeTitlesResponse.class);
	}

	public TmdbImageResponse images(int tvId) {
		return get("/tv/" + tvId + "/images", TmdbImageResponse.class);
	}

	private <T> T get(String path, Class<T> responseType) {
		return RetrySupport.withDefaultRetry(() -> getOnce(path, responseType));
	}

	private <T> T getOnce(String path, Class<T> responseType) {
		if (properties.getTmdb().getReadToken() == null || properties.getTmdb().getReadToken().isBlank()) {
			throw new NonRetryableRemoteException("TMDB_READ_TOKEN is not configured");
		}
		rateLimiter.acquire();
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getTmdb().getBaseUrl()) + path))
					.timeout(Duration.ofSeconds(30))
					.header("Authorization", "Bearer " + properties.getTmdb().getReadToken())
					.header("Accept", "application/json")
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status == 200) {
				return objectMapper.readValue(response.body(), responseType);
			}
			if (status == 429) {
				throw new RetryableRemoteException("TMDB returned HTTP 429", retryAfter(response));
			}
			if (status >= 500 && status <= 504) {
				throw new RetryableRemoteException("TMDB returned HTTP " + status);
			}
			throw new NonRetryableRemoteException("TMDB returned HTTP " + status);
		} catch (IOException e) {
			throw new RetryableRemoteException("TMDB request failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RetryableRemoteException("TMDB request interrupted", e);
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

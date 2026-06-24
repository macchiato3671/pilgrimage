package com.moonback.pilgrimage.batch.ingest.crawl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.moonback.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.moonback.pilgrimage.batch.ingest.support.NonRetryableRemoteException;
import com.moonback.pilgrimage.batch.ingest.support.RetrySupport;
import com.moonback.pilgrimage.batch.ingest.support.RetryableRemoteException;

@Component
public class TistoryPostHtmlFetcher {

	private final PilgrimageProperties properties;
	private final HttpClient httpClient;

	public TistoryPostHtmlFetcher(PilgrimageProperties properties) {
		this.properties = properties;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.getCrawler().getConnectTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public String fetch(String postUrl) {
		return RetrySupport.withDefaultRetry(() -> fetchOnce(postUrl));
	}

	private String fetchOnce(String postUrl) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(postUrl))
					.timeout(properties.getCrawler().getReadTimeout())
					.header("User-Agent", properties.getCrawler().getUserAgent())
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status == 200) {
				return response.body();
			}
			if (status == 429) {
				throw new RetryableRemoteException("Tistory returned HTTP 429", retryAfter(response));
			}
			if (status >= 500 && status <= 504) {
				throw new RetryableRemoteException("Tistory returned HTTP " + status);
			}
			throw new NonRetryableRemoteException("Tistory returned HTTP " + status);
		} catch (IOException e) {
			throw new RetryableRemoteException("Tistory post request failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RetryableRemoteException("Tistory post request interrupted", e);
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
}

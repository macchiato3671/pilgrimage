package com.ssafy.pilgrimage.batch.ingest.crawl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.model.DiscoveredPost;
import com.ssafy.pilgrimage.batch.ingest.support.CanonicalUrl;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;
import com.ssafy.pilgrimage.batch.ingest.support.NonRetryableRemoteException;
import com.ssafy.pilgrimage.batch.ingest.support.RetrySupport;
import com.ssafy.pilgrimage.batch.ingest.support.RetryableRemoteException;
import com.ssafy.pilgrimage.batch.ingest.support.SimpleRateLimiter;

import lombok.Builder;

@Component
public class TistoryPostDiscoverer {

	private final PilgrimageProperties properties;
	private final HttpClient httpClient;

	public TistoryPostDiscoverer(PilgrimageProperties properties) {
		this.properties = properties;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.getCrawler().getConnectTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public DiscoveryPage discoverPage(int page) {
		String pageUrl = pageUrl(page);
		String html = RetrySupport.withDefaultRetry(() -> fetch(pageUrl));
		Document document = Jsoup.parse(html, pageUrl);
		Map<String, DiscoveredPost> posts = new LinkedHashMap<>();
		for (Element link : document.select("a[href]")) {
			String href = link.attr("href");
			String canonical = CanonicalUrl.canonicalize(href, pageUrl);
			if (!looksLikePostUrl(canonical)) {
				continue;
			}
			String title = link.text().trim();
			posts.putIfAbsent(canonical, new DiscoveredPost(Hashing.sha256(canonical), canonical, title.isBlank() ? null : title));
		}
		boolean hasNext = document.select("a[href]").stream().anyMatch(this::looksLikeNextLink);
		return new DiscoveryPage(pageUrl, new ArrayList<>(posts.values()), hasNext);
	}

	public void delayBetweenRequests() {
		SimpleRateLimiter.sleep(Duration.ofMillis(Math.max(0, properties.getCrawler().getRequestDelayMs())));
	}

	private String fetch(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
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
			throw new RetryableRemoteException("Tistory request failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RetryableRemoteException("Tistory request interrupted", e);
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

	private String pageUrl(int page) {
		String categoryUrl = properties.getCrawler().getCategoryUrl();
		if (page <= 1) {
			return categoryUrl;
		}
		return categoryUrl + (categoryUrl.contains("?") ? "&" : "?") + "page=" + page;
	}

	private boolean looksLikePostUrl(String canonical) {
		URI uri = URI.create(canonical);
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
		return host.equals("ys-dl.tistory.com")
				&& !path.startsWith("/category")
				&& !path.startsWith("/tag")
				&& !path.startsWith("/guestbook")
				&& !path.equals("/")
				&& (path.matches("/\\d+.*") || path.startsWith("/entry/"));
	}

	private boolean looksLikeNextLink(Element link) {
		String text = link.text();
		String clazz = link.className();
		return text.contains("다음") || text.contains("Next") || clazz.toLowerCase(Locale.ROOT).contains("next");
	}

	@Builder
	public record DiscoveryPage(String pageUrl, List<DiscoveredPost> posts, boolean hasNext) {
	}
}

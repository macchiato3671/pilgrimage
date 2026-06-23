package com.ssafy.pilgrimage.batch.ingest.support;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CanonicalUrl {

	private static final List<String> TRACKING_PREFIXES = List.of("utm_");
	private static final List<String> TRACKING_NAMES = List.of("fbclid", "gclid", "igshid", "ref", "spm");

	private CanonicalUrl() {
	}

	public static String canonicalize(String rawUrl, String baseUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			return "";
		}
		String trimmed = rawUrl.trim();
		if (trimmed.startsWith("//")) {
			trimmed = "https:" + trimmed;
		}
		URI base = baseUrl == null || baseUrl.isBlank() ? null : URI.create(baseUrl);
		URI uri = base == null ? URI.create(trimmed) : base.resolve(trimmed);
		String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		int port = defaultPort(scheme, uri.getPort());
		String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
		String query = canonicalQuery(uri.getRawQuery());
		String authority = port < 0 ? host : host + ":" + port;
		return scheme + "://" + authority + path + (query == null ? "" : "?" + query);
	}

	private static int defaultPort(String scheme, int port) {
		if (("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80)) {
			return -1;
		}
		return port;
	}

	private static String canonicalQuery(String rawQuery) {
		if (rawQuery == null || rawQuery.isBlank()) {
			return null;
		}
		List<QueryParam> params = new ArrayList<>();
		for (String token : rawQuery.split("&")) {
			if (token.isBlank()) {
				continue;
			}
			String[] parts = token.split("=", 2);
			String name = decode(parts[0]);
			if (isTracking(name)) {
				continue;
			}
			String value = parts.length == 2 ? decode(parts[1]) : "";
			params.add(new QueryParam(name, value));
		}
		if (params.isEmpty()) {
			return null;
		}
		params.sort(Comparator.comparing(QueryParam::name).thenComparing(QueryParam::value));
		List<String> encoded = new ArrayList<>();
		for (QueryParam param : params) {
			encoded.add(encode(param.name()) + "=" + encode(param.value()));
		}
		return String.join("&", encoded);
	}

	private static boolean isTracking(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return TRACKING_NAMES.contains(lower) || TRACKING_PREFIXES.stream().anyMatch(lower::startsWith);
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private record QueryParam(String name, String value) {
	}
}

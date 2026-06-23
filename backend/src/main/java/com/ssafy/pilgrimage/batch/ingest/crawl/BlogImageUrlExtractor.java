package com.ssafy.pilgrimage.batch.ingest.crawl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.batch.ingest.support.CanonicalUrl;

@Component
public class BlogImageUrlExtractor {

	private static final List<String> IMAGE_ATTRIBUTES = List.of("data-original", "data-src", "data-url", "src");

	public List<String> extract(Element block, String baseUrl) {
		Set<String> urls = new LinkedHashSet<>();
		for (Element image : block.select("img")) {
			for (String attribute : IMAGE_ATTRIBUTES) {
				int before = urls.size();
				addIfValid(urls, image.attr(attribute), baseUrl);
				if (urls.size() > before) {
					break;
				}
			}
			Element wrappingLink = image.closest("a[href]");
			if (wrappingLink != null) {
				addIfValid(urls, wrappingLink.attr("href"), baseUrl);
			}
		}
		return List.copyOf(urls);
	}

	private void addIfValid(Set<String> urls, String candidate, String baseUrl) {
		if (candidate == null || candidate.isBlank()) {
			return;
		}
		String trimmed = candidate.trim();
		if (trimmed.startsWith("data:")) {
			return;
		}
		String lower = trimmed.toLowerCase(Locale.ROOT);
		if (lower.contains("blank") || lower.contains("spacer") || lower.contains("tracking")
				|| lower.contains("profile") || lower.contains("icon") || lower.contains("advert")
				|| lower.contains("/ads/")) {
			return;
		}
		String canonical = CanonicalUrl.canonicalize(trimmed, baseUrl);
		String canonicalLower = canonical.toLowerCase(Locale.ROOT);
		if (canonicalLower.endsWith(".ico") || canonicalLower.contains("1x1")) {
			return;
		}
		urls.add(canonical);
	}
}

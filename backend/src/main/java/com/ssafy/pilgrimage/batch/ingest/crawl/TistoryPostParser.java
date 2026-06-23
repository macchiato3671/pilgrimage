package com.ssafy.pilgrimage.batch.ingest.crawl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.batch.ingest.model.CrawlPostRow;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedPost;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedScene;
import com.ssafy.pilgrimage.batch.ingest.support.CanonicalUrl;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;
import com.ssafy.pilgrimage.batch.ingest.tmdb.DramaTitleNormalizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TistoryPostParser {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final String STOP_RECOMMENDATION = "주연배우들의또다른작품촬영지";
	private static final List<String> BODY_CANDIDATE_SELECTORS = List.of(
			"article",
			".tt_article_useless_p_margin",
			".contents_style",
			".article-view",
			".entry-content",
			".post-content",
			".contents",
			"body");

	private final BlogImageUrlExtractor imageUrlExtractor;
	private final DramaTitleNormalizer titleNormalizer;

	public ParsedPost parse(CrawlPostRow post, String html) {
		String canonicalUrl = CanonicalUrl.canonicalize(post.postUrl(), post.postUrl());
		Document document = Jsoup.parse(html, canonicalUrl);
		Element body = selectBody(document);
		String dramaQuery = extractDramaQuery(body, document, post.postTitle());
		String normalizedQuery = titleNormalizer.normalizeTitle(dramaQuery);
		byte[] bodyHash = Hashing.sha256(normalizedBodyHtml(body));
		List<ParsedScene> scenes = parseScenes(canonicalUrl, body);
		if (scenes.isEmpty()) {
			throw new IllegalArgumentException("No scene blocks found in post " + canonicalUrl);
		}
		return new ParsedPost(post.postKey(), canonicalUrl, dramaQuery, normalizedQuery, bodyHash, scenes);
	}

	private Element selectBody(Document document) {
		Set<Element> candidates = new LinkedHashSet<>();
		for (String selector : BODY_CANDIDATE_SELECTORS) {
			candidates.addAll(document.select(selector));
		}
		return candidates.stream()
				.max(Comparator.comparingInt(this::bodyScore))
				.orElse(document.body());
	}

	private int bodyScore(Element element) {
		int headings = element.select("h3,h4").size();
		int h2 = element.select("h2").size();
		int images = element.select("img").size();
		int textWeight = Math.min(50, element.text().length() / 200);
		return headings * 20 + h2 * 5 + images + textWeight;
	}

	private String extractDramaQuery(Element body, Document document, String listingTitle) {
		for (Element h2 : body.select("h2")) {
			String text = cleanText(h2.text());
			if (!text.isBlank() && !isStopH2(text) && !containsStopText(text)) {
				return titleNormalizer.toSearchQuery(text);
			}
		}
		Element h1 = document.selectFirst("h1");
		if (h1 != null && !cleanText(h1.text()).isBlank()) {
			return titleNormalizer.toSearchQuery(h1.text());
		}
		return titleNormalizer.toSearchQuery(listingTitle);
	}

	private List<ParsedScene> parseScenes(String canonicalUrl, Element body) {
		List<ParsedScene> scenes = new ArrayList<>();
		Map<String, Integer> occurrenceByName = new HashMap<>();
		Elements headings = body.select("h3,h4");
		for (Element heading : headings) {
			if (isAfterStop(heading)) {
				break;
			}
			String rawName = cleanText(heading.text());
			if (rawName.isBlank() || containsStopText(rawName)) {
				break;
			}
			Element block = new Element("section");
			boolean shouldStop = false;
			Element current = heading.nextElementSibling();
			while (current != null) {
				if (isSceneHeading(current) || isStopElement(current)) {
					shouldStop = isStopElement(current);
					break;
				}
				block.appendChild(current.clone());
				current = current.nextElementSibling();
			}
			String rawText = cleanText(block.text());
			if (!rawText.isBlank()) {
				String normalizedName = normalizeSceneName(rawName);
				int occurrence = occurrenceByName.merge(normalizedName, 1, Integer::sum);
				byte[] sourceKey = Hashing.sha256(canonicalUrl + "\0" + normalizedName + "\0" + occurrence);
				scenes.add(new ParsedScene(sourceKey, scenes.size() + 1, occurrence, rawName, normalizedName, rawText,
						rawText, imageUrlExtractor.extract(block, canonicalUrl)));
			}
			if (shouldStop) {
				break;
			}
		}
		return scenes;
	}

	private boolean isAfterStop(Element heading) {
		for (Element previous = heading.previousElementSibling(); previous != null; previous = previous.previousElementSibling()) {
			if (isStopElement(previous)) {
				return true;
			}
		}
		return false;
	}

	private boolean isStopElement(Element element) {
		if ("h2".equalsIgnoreCase(element.tagName()) && isStopH2(element.text())) {
			return true;
		}
		return containsStopText(element.text());
	}

	private boolean isStopH2(String text) {
		return cleanText(text).replaceAll("\\s+", "").equals(STOP_RECOMMENDATION);
	}

	private boolean containsStopText(String text) {
		String cleaned = cleanText(text);
		return cleaned.contains("공유하기") || cleaned.contains("게시글 관리");
	}

	private boolean isSceneHeading(Element element) {
		String tag = element.tagName().toLowerCase();
		return "h3".equals(tag) || "h4".equals(tag);
	}

	private String normalizedBodyHtml(Element body) {
		Element clone = body.clone();
		clone.select("script,style,iframe,noscript").remove();
		for (Element element : clone.select("*")) {
			element.removeAttr("style");
			element.removeAttr("onclick");
		}
		return WHITESPACE.matcher(clone.html()).replaceAll(" ").trim();
	}

	private String normalizeSceneName(String text) {
		return Normalizer.normalize(cleanText(text), Normalizer.Form.NFKC)
				.toLowerCase()
				.replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private String cleanText(String text) {
		if (text == null) {
			return "";
		}
		return WHITESPACE.matcher(text.replace('\u00A0', ' ').replace('\u200B', ' ').trim()).replaceAll(" ").trim();
	}
}

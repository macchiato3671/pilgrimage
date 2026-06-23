package com.moonback.pilgrimage.batch.ingest.geocode;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import lombok.Builder;

@Component
public class KoreanAddressNormalizer {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern STATUS = Pattern.compile("(폐업|추정|CG|장소이전|주소공개\\s*X|주소\\s*공개\\s*X)");
	private static final Pattern TRANSIT = Pattern.compile(
			"((?:[가-힣A-Za-z0-9]+역)\\s*\\d*번?\\s*출구|도보\\s*\\d+\\s*분|버스\\s*[^,，/]+|바로\\s*앞|근처|인근)");
	private static final Pattern ADDRESS = Pattern.compile(
			"((?:서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주|서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|경기도|강원특별자치도|강원도|충청북도|충청남도|전북특별자치도|전라북도|전라남도|경상북도|경상남도|제주특별자치도|제주도)\\s+[^\\n,，/]+?(?:\\s(?:\\d{1,5}(?:-\\d{1,5})?|[가-힣A-Za-z0-9-]+(?:로|길)\\s*\\d{0,5}(?:-\\d{1,5})?)))");

	public NormalizedAddress normalize(String raw) {
		String text = clean(raw);
		if (text.contains("/")) {
			text = text.split("/", 2)[0].trim();
		}

		List<String> notes = new ArrayList<>();
		text = extractMatches(text, STATUS, notes);
		text = extractMatches(text, TRANSIT, notes);

		String address = null;
		Matcher matcher = ADDRESS.matcher(text);
		if (matcher.find()) {
			address = clean(matcher.group(1));
			text = (text.substring(0, matcher.start()) + " " + text.substring(matcher.end())).trim();
		}

		if (address == null || address.isBlank()) {
			address = text;
			text = "";
		}

		if (!text.isBlank()) {
			notes.add(clean(text));
		}
		String description = String.join(" · ", notes.stream().filter(s -> !s.isBlank()).distinct().toList());
		if (description.length() > 255) {
			description = description.substring(0, 255);
		}
		return new NormalizedAddress(address, description);
	}

	private String extractMatches(String text, Pattern pattern, List<String> notes) {
		Matcher matcher = pattern.matcher(text);
		StringBuilder remaining = new StringBuilder();
		while (matcher.find()) {
			notes.add(formatNote(matcher.group(1)));
			matcher.appendReplacement(remaining, " ");
		}
		matcher.appendTail(remaining);
		return clean(remaining.toString());
	}

	private String formatNote(String note) {
		return clean(note)
				.replaceAll("([가-힣A-Za-z0-9]+역)(\\d+번?\\s*출구)", "$1 $2")
				.replaceAll("(\\d+)번\\s*출구", "$1번 출구")
				.replaceAll("(도보)(\\d+)", "$1 $2");
	}

	private String clean(String raw) {
		if (raw == null) {
			return "";
		}
		String unescaped = Parser.unescapeEntities(raw, false);
		String normalized = Normalizer.normalize(unescaped, Normalizer.Form.NFKC)
				.replace('\u00A0', ' ')
				.replace('\u200B', ' ')
				.replace('\n', ' ')
				.replace('\r', ' ')
				.trim();
		return WHITESPACE.matcher(normalized).replaceAll(" ").trim();
	}

	@Builder
	public record NormalizedAddress(String address, String description) {
	}
}

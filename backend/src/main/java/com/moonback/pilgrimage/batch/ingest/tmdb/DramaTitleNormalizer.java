package com.moonback.pilgrimage.batch.ingest.tmdb;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class DramaTitleNormalizer {

	private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern COUNT_GUIDE = Pattern.compile("\\b\\d+\\s*곳을\\s*알려드립니다");
	private static final Pattern UPDATE = Pattern.compile("업데이트\\s*중?");
	private static final Pattern FILMING = Pattern.compile("(드라마\\s*)?촬영지|촬영장소|촬영\\s*장소|촬영\\s*위치");
	private static final Pattern GUIDE = Pattern.compile("모음|정리|총정리|가는\\s*법|위치|주소|정보|리스트");
	private static final Pattern PARENS = Pattern.compile("[()\\[\\]{}【】〈〉《》]");
	private static final List<String> REGION_WORDS = List.of(
			"서울", "서울특별시", "경기", "경기도", "인천", "인천광역시", "부산", "부산광역시",
			"대구", "대구광역시", "광주", "광주광역시", "대전", "대전광역시", "울산", "울산광역시",
			"세종", "세종특별자치시", "강원", "강원도", "강원특별자치도", "충북", "충청북도",
			"충남", "충청남도", "전북", "전라북도", "전북특별자치도", "전남", "전라남도",
			"경북", "경상북도", "경남", "경상남도", "제주", "제주도", "제주특별자치도");

	public String toSearchQuery(String value) {
		if (value == null) {
			return "";
		}
		String text = normalizeWhitespace(value);
		text = COUNT_GUIDE.matcher(text).replaceAll(" ");
		text = UPDATE.matcher(text).replaceAll(" ");
		text = FILMING.matcher(text).replaceAll(" ");
		text = GUIDE.matcher(text).replaceAll(" ");
		for (String region : REGION_WORDS) {
			text = text.replace(region, " ");
		}
		text = PARENS.matcher(text).replaceAll(" ");
		text = text.replaceAll("[|:：,，·]", " ");
		text = normalizeWhitespace(text);
		return text;
	}

	public String normalizeTitle(String value) {
		String query = toSearchQuery(value);
		String lower = query.toLowerCase(Locale.ROOT);
		return lower.replaceAll("[^\\p{L}\\p{N}]", "");
	}

	public String normalizeRawTitle(String value) {
		if (value == null) {
			return "";
		}
		String lower = normalizeWhitespace(value).toLowerCase(Locale.ROOT);
		return lower.replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private String normalizeWhitespace(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
				.replace('\u00A0', ' ')
				.replace('\u200B', ' ')
				.trim();
		return REPEATED_WHITESPACE.matcher(normalized).replaceAll(" ").trim();
	}
}

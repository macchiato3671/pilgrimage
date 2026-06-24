package com.ssafy.pilgrimage.batch.ingest.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DramaTitleNormalizerTest {

	private final DramaTitleNormalizer normalizer = new DramaTitleNormalizer();

	@Test
	void removesFilmingLocationGuidePhrases() {
		assertThat(normalizer.toSearchQuery("참교육 촬영지 63곳을 알려드립니다")).isEqualTo("참교육");
		assertThat(normalizer.normalizeTitle("참교육 촬영지 63곳을 알려드립니다")).isEqualTo("참교육");
	}

	@Test
	void preservesNumbersThatBelongToTitle() {
		assertThat(normalizer.toSearchQuery("응답하라 1988 촬영지")).isEqualTo("응답하라 1988");
		assertThat(normalizer.normalizeTitle("응답하라 1988 촬영지")).isEqualTo("응답하라1988");
	}

	@Test
	void handlesUpdateRegionWhitespaceAndPunctuation() {
		assertThat(normalizer.toSearchQuery(" 서울  (선재 업고 튀어) 촬영지 업데이트중 "))
				.isEqualTo("선재 업고 튀어");
		assertThat(normalizer.normalizeTitle(" 서울  (선재 업고 튀어) 촬영지 업데이트중 "))
				.isEqualTo("선재업고튀어");
	}
}

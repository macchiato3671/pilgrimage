package com.ssafy.pilgrimage.batch.ingest.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ssafy.pilgrimage.batch.ingest.tmdb.TmdbDramaMatcher.CandidateWithAlternatives;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbAlternativeTitle;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbTvSearchResult;

class TmdbDramaMatcherTest {

	private final TmdbDramaMatcher matcher = new TmdbDramaMatcher(new DramaTitleNormalizer());

	@Test
	void approvesExactMatchWithSufficientGap() {
		var exact = candidate(1, "참교육", "True Lesson", "ko", List.of("KR"), "2025-01-01");
		var weak = candidate(2, "참 좋은 시절", "Wonderful Days", "ko", List.of("KR"), "2014-01-01");

		var decision = matcher.decide("참교육 촬영지", List.of(
				new CandidateWithAlternatives(exact, List.of()),
				new CandidateWithAlternatives(weak, List.of())));

		assertThat(decision.approved()).isTrue();
		assertThat(decision.selected().result().id()).isEqualTo(1);
	}

	@Test
	void rejectsTiedAmbiguousCandidates() {
		var first = candidate(1, "시그널", "Signal", "ko", List.of("KR"), "2016-01-01");
		var second = candidate(2, "시그널", "Signal", "ko", List.of("KR"), "2018-01-01");

		var decision = matcher.decide("시그널 촬영지", List.of(
				new CandidateWithAlternatives(first, List.of()),
				new CandidateWithAlternatives(second, List.of())));

		assertThat(decision.approved()).isFalse();
		assertThat(decision.notFound()).isFalse();
	}

	@Test
	void scoresAlternativeTitleExactMatch() {
		var result = candidate(10, "Lovely Runner", "선재 업고 튀어", "ko", List.of("KR"), "2024-04-08");

		var scored = matcher.score("러블리러너", 2024,
				new CandidateWithAlternatives(result, List.of(new TmdbAlternativeTitle("KR", "러블리 러너", ""))));

		assertThat(scored.score()).isGreaterThanOrEqualTo(120);
	}

	private TmdbTvSearchResult candidate(int id, String name, String originalName, String language,
			List<String> countries, String firstAirDate) {
		return new TmdbTvSearchResult(id, name, originalName, language, countries, firstAirDate, 1.0);
	}
}

package com.moonback.pilgrimage.batch.ingest.tmdb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.moonback.pilgrimage.batch.ingest.tmdb.dto.TmdbAlternativeTitle;
import com.moonback.pilgrimage.batch.ingest.tmdb.dto.TmdbTvSearchResult;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TmdbDramaMatcher {

	private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
	private final DramaTitleNormalizer titleNormalizer;

	public MatchDecision decide(String query, List<CandidateWithAlternatives> candidates) {
		String normalizedQuery = titleNormalizer.normalizeTitle(query);
		Integer postYear = extractYear(query).orElse(null);
		List<ScoredCandidate> scored = new ArrayList<>();
		for (CandidateWithAlternatives candidate : candidates) {
			scored.add(score(normalizedQuery, postYear, candidate));
		}
		scored.sort(Comparator.comparingInt(ScoredCandidate::score).reversed()
				.thenComparing(candidate -> -candidate.result().popularity()));
		if (scored.isEmpty()) {
			return MatchDecision.noResults();
		}
		ScoredCandidate first = scored.getFirst();
		int secondScore = scored.size() > 1 ? scored.get(1).score() : Integer.MIN_VALUE;
		if (first.score() >= 100 && first.score() - secondScore >= 15) {
			return MatchDecision.approved(first);
		}
		return MatchDecision.ambiguous(scored);
	}

	public ScoredCandidate score(String normalizedQuery, Integer postYear, CandidateWithAlternatives candidate) {
		int score = 0;
		TmdbTvSearchResult result = candidate.result();
		if (normalizedQuery.equals(titleNormalizer.normalizeRawTitle(result.name()))) {
			score += 100;
		}
		if (normalizedQuery.equals(titleNormalizer.normalizeRawTitle(result.originalName()))) {
			score += 100;
		}
		for (TmdbAlternativeTitle alternative : candidate.alternatives()) {
			if (normalizedQuery.equals(titleNormalizer.normalizeRawTitle(alternative.title()))) {
				score += 95;
				break;
			}
		}
		if (result.originCountry() != null && result.originCountry().contains("KR")) {
			score += 10;
		}
		if ("ko".equals(result.originalLanguage())) {
			score += 5;
		}
		if (postYear != null && result.firstAirDate() != null && result.firstAirDate().startsWith(String.valueOf(postYear))) {
			score += 10;
		}
		return new ScoredCandidate(result, score, candidate.alternatives());
	}

	private Optional<Integer> extractYear(String text) {
		if (text == null) {
			return Optional.empty();
		}
		Matcher matcher = YEAR.matcher(text);
		if (!matcher.find()) {
			return Optional.empty();
		}
		return Optional.of(Integer.parseInt(matcher.group()));
	}

	@Builder
	public record CandidateWithAlternatives(TmdbTvSearchResult result, List<TmdbAlternativeTitle> alternatives) {
	}

	@Builder
	public record ScoredCandidate(TmdbTvSearchResult result, int score, List<TmdbAlternativeTitle> alternatives) {
	}

	@Builder
	public record MatchDecision(boolean approved, boolean notFound, ScoredCandidate selected,
			List<ScoredCandidate> ranked) {
		public static MatchDecision approved(ScoredCandidate selected) {
			return new MatchDecision(true, false, selected, List.of(selected));
		}

		public static MatchDecision noResults() {
			return new MatchDecision(false, true, null, List.of());
		}

		public static MatchDecision ambiguous(List<ScoredCandidate> ranked) {
			return new MatchDecision(false, false, null, List.copyOf(ranked));
		}
	}
}

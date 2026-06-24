package com.ssafy.pilgrimage.batch.ingest.tmdb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.model.CrawlPostRow;
import com.ssafy.pilgrimage.batch.ingest.model.DramaUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.GenreUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.ImageTaskUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.ImageType;
import com.ssafy.pilgrimage.batch.ingest.model.IngestStatus;
import com.ssafy.pilgrimage.batch.ingest.model.OwnerType;
import com.ssafy.pilgrimage.batch.ingest.persistence.PilgrimageIngestRepository;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;
import com.ssafy.pilgrimage.batch.ingest.tmdb.TmdbDramaMatcher.CandidateWithAlternatives;
import com.ssafy.pilgrimage.batch.ingest.tmdb.TmdbDramaMatcher.MatchDecision;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbImageDto;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbImageResponse;
import com.ssafy.pilgrimage.batch.ingest.tmdb.dto.TmdbTvDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TmdbSyncService {

	private static final Logger log = LoggerFactory.getLogger(TmdbSyncService.class);

	private final PilgrimageIngestRepository repository;
	private final TmdbClient tmdbClient;
	private final TmdbDramaMatcher matcher;
	private final DramaTitleNormalizer titleNormalizer;
	private final PilgrimageProperties properties;

	public void process(CrawlPostRow post) {
		String query = firstNonBlank(post.dramaQuery(), post.postTitle());
		String normalized = titleNormalizer.normalizeTitle(query);
		try {
			var override = repository.findOverrideTmdbId(normalized);
			if (override.isPresent()) {
				TmdbTvDetail detail = tmdbClient.detail(override.get(), properties.getTmdb().getLanguage());
				saveMatched(post, detail);
				return;
			}

			var search = tmdbClient.searchTv(query);
			if (search.results() == null || search.results().isEmpty()) {
				repository.markTmdbStatus(post.postKey(), IngestStatus.TMDB_NOT_FOUND, "TMDB_NOT_FOUND",
						"No TMDB TV search results for " + query);
				return;
			}

			List<CandidateWithAlternatives> candidates = new ArrayList<>();
			for (var result : search.results().stream().limit(10).toList()) {
				var alternatives = tmdbClient.alternativeTitles(result.id()).results();
				candidates.add(new CandidateWithAlternatives(result, alternatives == null ? List.of() : alternatives));
			}
			MatchDecision decision = matcher.decide(query, candidates);
			if (decision.notFound()) {
				repository.markTmdbStatus(post.postKey(), IngestStatus.TMDB_NOT_FOUND, "TMDB_NOT_FOUND",
						"No TMDB TV search results for " + query);
				return;
			}
			if (!decision.approved()) {
				String top = decision.ranked().stream()
						.limit(3)
						.map(candidate -> candidate.result().id() + ":" + candidate.score())
						.toList()
						.toString();
				repository.markTmdbStatus(post.postKey(), IngestStatus.TMDB_AMBIGUOUS, "TMDB_AMBIGUOUS",
						"Ambiguous TMDB candidates " + top);
				return;
			}

			TmdbTvDetail detail = tmdbClient.detail(decision.selected().result().id(), properties.getTmdb().getLanguage());
			saveMatched(post, detail);
		} catch (RuntimeException e) {
			log.warn("TMDB sync failed for postKey={} status=FAILED error={}", Hashing.hex(post.postKey()), e.toString());
			repository.markPostFailure(post.postKey(), "TMDB_FAILED", e.getMessage());
		}
	}

	private void saveMatched(CrawlPostRow post, TmdbTvDetail koreanDetail) {
		TmdbTvDetail englishDetail = null;
		if (isBlank(koreanDetail.overview())) {
			englishDetail = tmdbClient.detail(koreanDetail.id(), "en-US");
		}
		TmdbImageResponse images = tmdbClient.images(koreanDetail.id());
		DramaUpsert drama = mapDrama(koreanDetail, englishDetail);
		List<ImageTaskUpsert> tasks = imageTasks(koreanDetail.id(), images);
		repository.saveTmdbMatched(post, drama, tasks);
	}

	private DramaUpsert mapDrama(TmdbTvDetail detail, TmdbTvDetail englishDetail) {
		String title = firstNonBlank(detail.name(), detail.originalName());
		String overview = firstNonBlank(detail.overview(), englishDetail == null ? null : englishDetail.overview());
		List<GenreUpsert> genres = detail.genres() == null ? List.of()
				: detail.genres().stream().map(genre -> new GenreUpsert(genre.id(), genre.name())).toList();
		return new DramaUpsert(
				detail.id(),
				title,
				detail.originalName(),
				detail.originalLanguage(),
				detail.originCountry() == null ? null : String.join(",", detail.originCountry()),
				parseDate(detail.firstAirDate()),
				isBlank(overview) ? null : overview,
				genres);
	}

	private List<ImageTaskUpsert> imageTasks(int tmdbId, TmdbImageResponse response) {
		List<ImageTaskUpsert> tasks = new ArrayList<>();
		addImageTasks(tasks, tmdbId, ImageType.POSTER, response == null ? null : response.posters());
		addImageTasks(tasks, tmdbId, ImageType.BACKDROP, response == null ? null : response.backdrops());
		addImageTasks(tasks, tmdbId, ImageType.LOGO, response == null ? null : response.logos());
		return tasks;
	}

	private void addImageTasks(List<ImageTaskUpsert> tasks, int tmdbId, ImageType type, List<TmdbImageDto> images) {
		if (images == null || images.isEmpty()) {
			return;
		}
		Set<String> seen = new HashSet<>();
		int order = 0;
		for (TmdbImageDto image : images) {
			if (image.filePath() == null || image.filePath().isBlank() || !languageAllowed(image.language())
					|| !seen.add(image.filePath())) {
				continue;
			}
			String sourceUrl = trimTrailingSlash(properties.getTmdb().getImageBaseUrl()) + image.filePath();
			byte[] taskKey = Hashing.sha256("TMDB\0" + tmdbId + "\0" + type.name() + "\0" + image.filePath());
			tasks.add(new ImageTaskUpsert(taskKey, OwnerType.DRAMA, tmdbId, type, sourceUrl, image.filePath(), order++));
		}
	}

	private boolean languageAllowed(String language) {
		String filter = properties.getTmdb().getImageLanguages();
		if (filter == null || filter.isBlank()) {
			return true;
		}
		Set<String> allowed = filter.lines()
				.flatMap(line -> java.util.Arrays.stream(line.split(",")))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.collect(java.util.stream.Collectors.toSet());
		return language == null || allowed.contains(language);
	}

	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return LocalDate.parse(value);
	}

	private String firstNonBlank(String first, String second) {
		return isBlank(first) ? second : first;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String trimTrailingSlash(String value) {
		return value == null ? "" : value.replaceAll("/+$", "");
	}
}

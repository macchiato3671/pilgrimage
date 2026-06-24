package com.ssafy.pilgrimage.batch.ingest.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.batch.ingest.geocode.GeocodeResult;
import com.ssafy.pilgrimage.batch.ingest.model.CrawlPostRow;
import com.ssafy.pilgrimage.batch.ingest.model.CrawlSceneRow;
import com.ssafy.pilgrimage.batch.ingest.model.DiscoveredPost;
import com.ssafy.pilgrimage.batch.ingest.model.DramaUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.ImageIngestTaskRow;
import com.ssafy.pilgrimage.batch.ingest.model.ImageTaskUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.ImageType;
import com.ssafy.pilgrimage.batch.ingest.model.ImageUploadResult;
import com.ssafy.pilgrimage.batch.ingest.model.IngestStatus;
import com.ssafy.pilgrimage.batch.ingest.model.OwnerType;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedPost;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedScene;
import com.ssafy.pilgrimage.batch.ingest.model.SceneSave;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PilgrimageIngestRepository {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};

	private final PilgrimageIngestMapper mapper;
	private final ObjectMapper objectMapper;

	public boolean existsPost(byte[] postKey) {
		return mapper.countPostByKey(postKey) > 0;
	}

	public void upsertDiscoveredPost(DiscoveredPost post) {
		mapper.upsertDiscoveredPost(post, IngestStatus.DISCOVERED.name());
	}

	public List<CrawlPostRow> findPostsForParsing(boolean retryFailed) {
		return mapper.findPostsForParsing(IngestStatus.DISCOVERED.name(), retryFailed, IngestStatus.FAILED.name())
				.stream()
				.map(this::toPostRow)
				.toList();
	}

	public List<CrawlPostRow> findPostsForTmdb(boolean retryFailed) {
		List<String> statuses = new ArrayList<>(List.of(IngestStatus.PARSED.name()));
		if (retryFailed) {
			statuses.add(IngestStatus.TMDB_NOT_FOUND.name());
			statuses.add(IngestStatus.TMDB_AMBIGUOUS.name());
			statuses.add(IngestStatus.FAILED.name());
		}
		return mapper.findPostsByStatuses(statuses).stream().map(this::toPostRow).toList();
	}

	@Transactional
	public void saveParsedPost(ParsedPost parsedPost) {
		mapper.updatePostParsed(parsedPost, IngestStatus.PARSED.name());
		mapper.shiftSceneOrders(parsedPost.postKey());
		for (ParsedScene scene : parsedPost.scenes()) {
			mapper.upsertParsedScene(parsedPost.postKey(), scene, toJson(scene.imageUrls()), IngestStatus.PARSED.name());
		}
		mapper.markRemovedScenes(parsedPost.postKey(), IngestStatus.FAILED.name(), "SOURCE_REMOVED",
				"Scene block no longer appears in the parsed post");
	}

	public void markPostFailure(byte[] postKey, String errorCode, String message) {
		mapper.markPostStatus(postKey, IngestStatus.FAILED.name(), errorCode, truncate(message, 1000));
	}

	public Optional<Integer> findOverrideTmdbId(String normalizedTitle) {
		return Optional.ofNullable(mapper.findOverrideTmdbId(normalizedTitle));
	}

	@Transactional
	public void saveTmdbMatched(CrawlPostRow post, DramaUpsert drama, List<ImageTaskUpsert> imageTasks) {
		mapper.upsertDrama(drama);
		for (var genre : drama.genres()) {
			mapper.upsertGenre(genre);
		}
		mapper.deleteDramaGenre(drama.dramaId());
		for (var genre : drama.genres()) {
			mapper.upsertDramaGenre(drama.dramaId(), genre.genreId());
		}
		mapper.updatePostTmdbMatched(post.postKey(), drama.dramaId(), IngestStatus.TMDB_MATCHED.name());
		imageTasks.forEach(this::upsertImageTask);
	}

	public void markTmdbStatus(byte[] postKey, IngestStatus status, String errorCode, String message) {
		mapper.markPostStatus(postKey, status.name(), errorCode, truncate(message, 1000));
	}

	public List<CrawlSceneRow> findScenesForSync(boolean retryFailed) {
		List<String> statuses = new ArrayList<>(List.of(IngestStatus.PARSED.name()));
		if (retryFailed) {
			statuses.add(IngestStatus.FAILED.name());
			statuses.add(IngestStatus.GEOCODE_NOT_FOUND.name());
			statuses.add(IngestStatus.GEOCODE_AMBIGUOUS.name());
		}
		return mapper.findScenesForSync(IngestStatus.TMDB_MATCHED.name(), statuses).stream()
				.map(this::toSceneRow)
				.toList();
	}

	@Transactional
	public int saveScene(SceneSave sceneSave, List<ImageTaskUpsert> imageTasks) {
		Integer sceneId = sceneSave.existingSceneId();
		if (sceneId == null) {
			sceneId = mapper.findSceneIdByIngestKey(sceneSave.ingestKey());
		}
		if (sceneId == null) {
			sceneId = insertScene(sceneSave);
		} else {
			mapper.updateScene(sceneId, sceneSave, truncate(sceneSave.description(), 255),
					truncate(sceneSave.address(), 255));
		}
		mapper.updateCrawlSceneSaved(sceneSave.sourceKey(), sceneId, IngestStatus.SCENE_SAVED.name());
		imageTasks.forEach(this::upsertImageTask);
		return sceneId;
	}

	private int insertScene(SceneSave sceneSave) {
		SceneInsertParam param = new SceneInsertParam();
		param.setDramaId(sceneSave.dramaId());
		param.setName(sceneSave.name());
		param.setDescription(truncate(sceneSave.description(), 255));
		param.setAddress(truncate(sceneSave.address(), 255));
		param.setLatitude(sceneSave.latitude());
		param.setLongitude(sceneSave.longitude());
		param.setIngestKey(sceneSave.ingestKey());
		mapper.insertScene(param);
		if (param.getSceneId() == null) {
			throw new IllegalStateException("Scene insert did not return a generated key");
		}
		return param.getSceneId();
	}

	public void markSceneStatus(byte[] sourceKey, IngestStatus status, String errorCode, String message) {
		mapper.markSceneStatus(sourceKey, status.name(), errorCode, truncate(message, 1000));
	}

	public Optional<GeocodeCacheHit> findFreshGeocodeCache(byte[] queryHash) {
		GeocodeCacheMapperRow row = mapper.findFreshGeocodeCache(queryHash);
		if (row == null) {
			return Optional.empty();
		}
		return Optional.of(new GeocodeCacheHit(row.getStatus(), row.getQueryText(), row.getCanonicalAddress(),
				row.getRegion1Depth(), row.getRegion2Depth(), row.getLatitude(), row.getLongitude(),
				row.getResponseJson()));
	}

	public void saveSuccessfulGeocodeCache(byte[] queryHash, String queryText, GeocodeResult result) {
		mapper.saveSuccessfulGeocodeCache(queryHash, queryText, result);
	}

	public void saveNoResultGeocodeCache(byte[] queryHash, String queryText) {
		mapper.saveNoResultGeocodeCache(queryHash, queryText);
	}

	public void upsertImageTask(ImageTaskUpsert task) {
		mapper.upsertImageTask(task, IngestStatus.IMAGE_PENDING.name());
	}

	public List<ImageIngestTaskRow> findImageTasksForUpload(boolean retryFailed) {
		List<String> statuses = new ArrayList<>(List.of(IngestStatus.IMAGE_PENDING.name()));
		if (retryFailed) {
			statuses.add(IngestStatus.FAILED.name());
		}
		return mapper.findImageTasksByStatuses(statuses).stream().map(this::toImageTaskRow).toList();
	}

	public int findDramaIdBySceneId(int sceneId) {
		Integer dramaId = mapper.findDramaIdBySceneId(sceneId);
		if (dramaId == null) {
			throw new IllegalStateException("Scene not found: " + sceneId);
		}
		return dramaId;
	}

	@Transactional
	public void saveImageUploaded(ImageIngestTaskRow task, ImageUploadResult result) {
		mapper.updateImageTaskCompleted(result, IngestStatus.COMPLETED.name());
		if (task.ownerType() == OwnerType.DRAMA) {
			mapper.upsertDramaImg(task, result);
		} else {
			mapper.upsertSceneImg(task, result);
		}
	}

	public void markImageFailure(byte[] taskKey, String errorCode, String message) {
		mapper.markImageFailure(taskKey, IngestStatus.FAILED.name(), errorCode, truncate(message, 1000));
	}

	public Map<String, Object> summarize() {
		return Map.ofEntries(
				Map.entry("discoveredPosts", mapper.countCrawlPostAll()),
				Map.entry("parsedPosts", mapper.countCrawlPostByStatus(IngestStatus.PARSED.name())),
				Map.entry("unparsedPosts", mapper.countCrawlPostStatusNot(IngestStatus.PARSED.name())),
				Map.entry("tmdbMatches", mapper.countCrawlPostByStatus(IngestStatus.TMDB_MATCHED.name())),
				Map.entry("tmdbNotFound", mapper.countCrawlPostByStatus(IngestStatus.TMDB_NOT_FOUND.name())),
				Map.entry("tmdbAmbiguous", mapper.countCrawlPostByStatus(IngestStatus.TMDB_AMBIGUOUS.name())),
				Map.entry("dramaRows", mapper.countDrama()),
				Map.entry("genreRows", mapper.countGenre()),
				Map.entry("savedScenes", mapper.countCrawlSceneByStatus(IngestStatus.SCENE_SAVED.name())),
				Map.entry("excludedForeignScenes", mapper.countCrawlSceneByStatus(IngestStatus.NON_DOMESTIC.name())),
				Map.entry("geocodeFailures", mapper.countCrawlSceneByStatuses(List.of(
						IngestStatus.GEOCODE_NOT_FOUND.name(), IngestStatus.GEOCODE_AMBIGUOUS.name()))),
				Map.entry("imageTasks", mapper.countImageTaskAll()),
				Map.entry("successfulImages", mapper.countImageTaskByStatus(IngestStatus.COMPLETED.name())),
				Map.entry("imageFailures", mapper.countImageTaskByStatus(IngestStatus.FAILED.name())),
				Map.entry("reusedS3Objects", mapper.countReusedS3Objects(IngestStatus.COMPLETED.name()))
		);
	}

	private CrawlPostRow toPostRow(CrawlPostMapperRow row) {
		return new CrawlPostRow(row.getPostKey(), row.getPostUrl(), row.getPostTitle(), row.getPublishedAt(),
				row.getDramaQuery(), row.getNormalizedQuery(), row.getTmdbId(), row.getContentHash(),
				status(row.getStatus()), row.getErrorCode(), row.getErrorMessage());
	}

	private CrawlSceneRow toSceneRow(CrawlSceneMapperRow row) {
		return new CrawlSceneRow(row.getSourceKey(), row.getPostKey(), row.getPostUrl(), row.getTmdbId(),
				row.getSourceOrder(), row.getSameNameOccurrence(), row.getRawName(), row.getRawText(),
				row.getRawAddress(), fromJsonList(row.getImageUrls()), row.getSceneId(), status(row.getStatus()),
				row.getErrorCode(), row.getErrorMessage());
	}

	private ImageIngestTaskRow toImageTaskRow(ImageIngestTaskMapperRow row) {
		return new ImageIngestTaskRow(row.getTaskKey(), OwnerType.valueOf(row.getOwnerType()), row.getOwnerId(),
				ImageType.valueOf(row.getImageType()), row.getSourceUrl(), row.getSourceIdentity(), row.getSortOrder(),
				row.getObjectKey(), row.getContentHash(), row.getWidth(), row.getHeight(), status(row.getStatus()),
				row.getAttemptCount(), row.getErrorCode(), row.getErrorMessage());
	}

	private IngestStatus status(String value) {
		return value == null ? IngestStatus.FAILED : IngestStatus.valueOf(value);
	}

	private String toJson(List<String> values) {
		try {
			return objectMapper.writeValueAsString(values == null ? List.of() : values);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to serialize JSON", e);
		}
	}

	private List<String> fromJsonList(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, STRING_LIST);
		} catch (JsonProcessingException e) {
			return List.of();
		}
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	public boolean sameHash(byte[] left, byte[] right) {
		return Arrays.equals(left, right);
	}
}

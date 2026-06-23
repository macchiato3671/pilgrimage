package com.ssafy.pilgrimage.batch.ingest.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ssafy.pilgrimage.batch.ingest.model.DiscoveredPost;
import com.ssafy.pilgrimage.batch.ingest.model.DramaUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.GenreUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.ImageIngestTaskRow;
import com.ssafy.pilgrimage.batch.ingest.model.ImageTaskUpsert;
import com.ssafy.pilgrimage.batch.ingest.model.ImageUploadResult;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedPost;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedScene;
import com.ssafy.pilgrimage.batch.ingest.model.SceneSave;

public interface PilgrimageIngestMapper {

	int countPostByKey(@Param("postKey") byte[] postKey);

	void upsertDiscoveredPost(@Param("post") DiscoveredPost post, @Param("status") String status);

	List<CrawlPostMapperRow> findPostsForParsing(@Param("discoveredStatus") String discoveredStatus,
			@Param("retryFailed") boolean retryFailed, @Param("failedStatus") String failedStatus);

	List<CrawlPostMapperRow> findPostsByStatuses(@Param("statuses") List<String> statuses);

	void updatePostParsed(@Param("post") ParsedPost post, @Param("status") String status);

	void shiftSceneOrders(@Param("postKey") byte[] postKey);

	void upsertParsedScene(@Param("postKey") byte[] postKey, @Param("scene") ParsedScene scene,
			@Param("imageUrlsJson") String imageUrlsJson, @Param("status") String status);

	void markRemovedScenes(@Param("postKey") byte[] postKey, @Param("status") String status,
			@Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

	void markPostStatus(@Param("postKey") byte[] postKey, @Param("status") String status,
			@Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

	Integer findOverrideTmdbId(@Param("normalizedTitle") String normalizedTitle);

	void upsertDrama(@Param("drama") DramaUpsert drama);

	void upsertGenre(@Param("genre") GenreUpsert genre);

	void deleteDramaGenre(@Param("dramaId") int dramaId);

	void upsertDramaGenre(@Param("dramaId") int dramaId, @Param("genreId") int genreId);

	void updatePostTmdbMatched(@Param("postKey") byte[] postKey, @Param("tmdbId") int tmdbId,
			@Param("status") String status);

	List<CrawlSceneMapperRow> findScenesForSync(@Param("postStatus") String postStatus,
			@Param("statuses") List<String> statuses);

	Integer findSceneIdByIngestKey(@Param("ingestKey") byte[] ingestKey);

	void insertScene(SceneInsertParam scene);

	void updateScene(@Param("sceneId") int sceneId, @Param("scene") SceneSave scene,
			@Param("description") String description, @Param("address") String address);

	void updateCrawlSceneSaved(@Param("sourceKey") byte[] sourceKey, @Param("sceneId") int sceneId,
			@Param("status") String status);

	void markSceneStatus(@Param("sourceKey") byte[] sourceKey, @Param("status") String status,
			@Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

	GeocodeCacheMapperRow findFreshGeocodeCache(@Param("queryHash") byte[] queryHash);

	void saveSuccessfulGeocodeCache(@Param("queryHash") byte[] queryHash, @Param("queryText") String queryText,
			@Param("result") com.ssafy.pilgrimage.batch.ingest.geocode.GeocodeResult result);

	void saveNoResultGeocodeCache(@Param("queryHash") byte[] queryHash, @Param("queryText") String queryText);

	void upsertImageTask(@Param("task") ImageTaskUpsert task, @Param("status") String status);

	List<ImageIngestTaskMapperRow> findImageTasksByStatuses(@Param("statuses") List<String> statuses);

	Integer findDramaIdBySceneId(@Param("sceneId") int sceneId);

	void updateImageTaskCompleted(@Param("result") ImageUploadResult result, @Param("status") String status);

	void upsertDramaImg(@Param("task") ImageIngestTaskRow task, @Param("result") ImageUploadResult result);

	void upsertSceneImg(@Param("task") ImageIngestTaskRow task, @Param("result") ImageUploadResult result);

	void markImageFailure(@Param("taskKey") byte[] taskKey, @Param("status") String status,
			@Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

	int countCrawlPostAll();

	int countCrawlPostByStatus(@Param("status") String status);

	int countCrawlPostStatusNot(@Param("status") String status);

	int countDrama();

	int countGenre();

	int countDramaGenre();

	int countCrawlSceneByStatus(@Param("status") String status);

	int countCrawlSceneByStatuses(@Param("statuses") List<String> statuses);

	int countImageTaskAll();

	int countImageTaskByStatus(@Param("status") String status);

	int countReusedS3Objects(@Param("status") String status);
}

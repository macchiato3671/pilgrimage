package com.moonback.pilgrimage.batch.ingest.geocode;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.batch.ingest.geocode.KoreanAddressNormalizer.NormalizedAddress;
import com.moonback.pilgrimage.batch.ingest.model.CrawlSceneRow;
import com.moonback.pilgrimage.batch.ingest.model.ImageTaskUpsert;
import com.moonback.pilgrimage.batch.ingest.model.ImageType;
import com.moonback.pilgrimage.batch.ingest.model.IngestStatus;
import com.moonback.pilgrimage.batch.ingest.model.OwnerType;
import com.moonback.pilgrimage.batch.ingest.model.SceneSave;
import com.moonback.pilgrimage.batch.ingest.persistence.PilgrimageIngestRepository;
import com.moonback.pilgrimage.batch.ingest.support.Hashing;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SceneSyncService {

	private static final Logger log = LoggerFactory.getLogger(SceneSyncService.class);

	private final KoreanAddressNormalizer addressNormalizer;
	private final GeocodeService geocodeService;
	private final PilgrimageIngestRepository repository;

	public void process(CrawlSceneRow scene) {
		try {
			NormalizedAddress normalizedAddress = addressNormalizer.normalize(scene.rawAddress());
			if (normalizedAddress.address() == null || normalizedAddress.address().isBlank()) {
				repository.markSceneStatus(scene.sourceKey(), IngestStatus.GEOCODE_NOT_FOUND, "ADDRESS_EMPTY",
						"No address candidate found in scene block");
				return;
			}
			GeocodeService.GeocodeOutcome outcome = geocodeService.geocode(normalizedAddress.address(), scene.rawName());
			if (!outcome.success()) {
				repository.markSceneStatus(scene.sourceKey(), outcome.status(), outcome.errorCode(), outcome.errorMessage());
				return;
			}

			GeocodeResult geocode = outcome.result();
			byte[] ingestKey = Hashing.sha256(scene.tmdbId() + "\0" + normalizeSceneName(scene.rawName()) + "\0"
					+ geocode.canonicalAddress());
			SceneSave save = new SceneSave(
					scene.sourceKey(),
					scene.tmdbId(),
					scene.rawName(),
					normalizedAddress.description(),
					geocode.canonicalAddress(),
					geocode.latitude(),
					geocode.longitude(),
					ingestKey,
					scene.sceneId());
			int sceneId = repository.saveScene(save, List.of());
			for (ImageTaskUpsert task : sceneImageTasks(scene, sceneId)) {
				repository.upsertImageTask(task);
			}
		} catch (RuntimeException e) {
			log.warn("Scene sync failed for sourceKey={} error={}", Hashing.hex(scene.sourceKey()), e.toString());
			repository.markSceneStatus(scene.sourceKey(), IngestStatus.FAILED, "SCENE_SYNC_FAILED", e.getMessage());
		}
	}

	private List<ImageTaskUpsert> sceneImageTasks(CrawlSceneRow scene, int sceneId) {
		List<ImageTaskUpsert> tasks = new ArrayList<>();
		int order = 0;
		for (String sourceUrl : scene.imageUrls()) {
			byte[] taskKey = Hashing.sha256("BLOG\0" + Hashing.hex(scene.sourceKey()) + "\0" + sourceUrl);
			tasks.add(new ImageTaskUpsert(taskKey, OwnerType.SCENE, sceneId, ImageType.SCENE, sourceUrl, null, order++));
		}
		return tasks;
	}

	private String normalizeSceneName(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value, Normalizer.Form.NFKC)
				.toLowerCase()
				.replaceAll("[^\\p{L}\\p{N}]", "");
	}
}

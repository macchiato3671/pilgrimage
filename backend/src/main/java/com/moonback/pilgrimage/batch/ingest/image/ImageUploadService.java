package com.moonback.pilgrimage.batch.ingest.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.batch.ingest.model.ImageIngestTaskRow;
import com.moonback.pilgrimage.batch.ingest.model.ImageType;
import com.moonback.pilgrimage.batch.ingest.model.ImageUploadResult;
import com.moonback.pilgrimage.batch.ingest.model.OwnerType;
import com.moonback.pilgrimage.batch.ingest.persistence.PilgrimageIngestRepository;
import com.moonback.pilgrimage.batch.ingest.support.Hashing;
import com.moonback.pilgrimage.batch.ingest.support.NonRetryableRemoteException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

	private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);

	private final ImageDownloader downloader;
	private final ImageInspector inspector;
	private final WebpConverter converter;
	private final ObjectKeyFactory objectKeyFactory;
	private final S3ImageStorage storage;
	private final PilgrimageIngestRepository repository;

	public void process(ImageIngestTaskRow task) {
		DownloadedImage downloaded = null;
		ConvertedImage converted = null;
		try {
			downloaded = downloader.download(task.sourceUrl());
			ImageInspection inspection = inspector.inspect(downloaded.path());
			converted = converter.convert(downloaded.path(), inspection, task.imageType());
			byte[] contentHash = Hashing.sha256(converted.path());
			String objectKey = objectKey(task, contentHash);
			boolean reused = storage.uploadIfMissing(objectKey, converted.path(), contentHash);
			String url = storage.urlFor(objectKey);
			ImageUploadResult result = new ImageUploadResult(task.taskKey(), objectKey, url, contentHash,
					Math.max(0, converted.width()), Math.max(0, converted.height()), reused);
			repository.saveImageUploaded(task, result);
		} catch (ImageProcessingException e) {
			repository.markImageFailure(task.taskKey(), e.errorCode(), e.getMessage());
		} catch (NonRetryableRemoteException e) {
			repository.markImageFailure(task.taskKey(), "IMAGE_UNSUPPORTED", e.getMessage());
		} catch (RuntimeException | IOException e) {
			log.warn("Image upload failed for taskKey={} error={}", Hashing.hex(task.taskKey()), e.toString());
			repository.markImageFailure(task.taskKey(), "IMAGE_FAILED", e.getMessage());
		} finally {
			deleteQuietly(downloaded == null ? null : downloaded.path());
			deleteQuietly(converted == null ? null : converted.path());
		}
	}

	private String objectKey(ImageIngestTaskRow task, byte[] contentHash) {
		if (task.ownerType() == OwnerType.DRAMA) {
			return objectKeyFactory.dramaKey(task.ownerId(), task.imageType(), contentHash);
		}
		if (task.imageType() != ImageType.SCENE) {
			throw new ImageProcessingException("IMAGE_OWNER_INVALID", "Scene owner must use SCENE image type");
		}
		int dramaId = repository.findDramaIdBySceneId(task.ownerId());
		return objectKeyFactory.sceneKey(dramaId, task.ownerId(), contentHash);
	}

	private void deleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// Best-effort cleanup only.
		}
	}
}

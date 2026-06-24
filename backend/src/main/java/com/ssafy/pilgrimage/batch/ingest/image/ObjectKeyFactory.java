package com.ssafy.pilgrimage.batch.ingest.image;

import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.model.ImageType;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ObjectKeyFactory {

	private final PilgrimageProperties properties;

	public String dramaKey(int dramaId, ImageType imageType, byte[] contentHash) {
		String folder = switch (imageType) {
			case POSTER -> "posters";
			case BACKDROP -> "backdrops";
			case LOGO -> "logos";
			case SCENE -> throw new IllegalArgumentException("Use sceneKey for scene images");
		};
		return normalizePrefix() + "/dramas/" + dramaId + "/" + folder + "/" + Hashing.hex(contentHash) + ".webp";
	}

	public String sceneKey(int dramaId, int sceneId, byte[] contentHash) {
		return normalizePrefix() + "/dramas/" + dramaId + "/scenes/" + sceneId + "/" + Hashing.hex(contentHash) + ".webp";
	}

	private String normalizePrefix() {
		String prefix = properties.getStorage().getPrefix();
		if (prefix == null || prefix.isBlank()) {
			return "pilgrimage";
		}
		return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
	}
}

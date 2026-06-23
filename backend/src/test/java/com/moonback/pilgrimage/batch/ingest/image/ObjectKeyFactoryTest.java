package com.moonback.pilgrimage.batch.ingest.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.moonback.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.moonback.pilgrimage.batch.ingest.model.ImageType;
import com.moonback.pilgrimage.batch.ingest.support.Hashing;

class ObjectKeyFactoryTest {

	@Test
	void sameBytesProduceSameDramaKey() {
		ObjectKeyFactory factory = factory();
		byte[] hash = Hashing.sha256("same-webp-bytes");

		assertThat(factory.dramaKey(1399, ImageType.POSTER, hash))
				.isEqualTo(factory.dramaKey(1399, ImageType.POSTER, hash));
	}

	@Test
	void ownerAndImageTypePrefixesAreCorrect() {
		ObjectKeyFactory factory = factory();
		byte[] hash = Hashing.sha256("image");

		assertThat(factory.dramaKey(1399, ImageType.BACKDROP, hash))
				.startsWith("pilgrimage/dramas/1399/backdrops/")
				.endsWith(".webp");
		assertThat(factory.sceneKey(1399, 1082, hash))
				.startsWith("pilgrimage/dramas/1399/scenes/1082/")
				.endsWith(".webp");
	}

	private ObjectKeyFactory factory() {
		PilgrimageProperties properties = new PilgrimageProperties();
		properties.getStorage().setPrefix("pilgrimage");
		return new ObjectKeyFactory(properties);
	}
}

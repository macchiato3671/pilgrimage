package com.ssafy.pilgrimage.batch.ingest.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.ssafy.pilgrimage.batch.ingest.model.CrawlPostRow;
import com.ssafy.pilgrimage.batch.ingest.model.IngestStatus;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;
import com.ssafy.pilgrimage.batch.ingest.tmdb.DramaTitleNormalizer;

class TistoryPostParserTest {

	private final TistoryPostParser parser = new TistoryPostParser(
			new BlogImageUrlExtractor(),
			new DramaTitleNormalizer());

	@Test
	void parsesRecentH3PostAndStopsBeforeRecommendationSection() throws Exception {
		var parsed = parser.parse(post("https://ys-dl.tistory.com/100", "참교육 촬영지"),
				fixture("pilgrimage/fixtures/recent-h3-post.html"));

		assertThat(parsed.dramaQuery()).isEqualTo("참교육");
		assertThat(parsed.scenes()).hasSize(2);
		assertThat(parsed.scenes().get(0).rawName()).isEqualTo("카페 문");
		assertThat(parsed.scenes().get(0).imageUrls()).containsExactly("https://images.example.com/scene1.jpg");
		assertThat(parsed.scenes().get(1).rawText()).contains("서울 영등포구");
		assertThat(parsed.scenes()).noneMatch(scene -> scene.rawName().contains("추천"));
	}

	@Test
	void parsesOlderH4PostAndWrappedImageLinks() throws Exception {
		var parsed = parser.parse(post("https://ys-dl.tistory.com/101", "선재 업고 튀어 촬영지"),
				fixture("pilgrimage/fixtures/older-h4-post.html"));

		assertThat(parsed.dramaQuery()).isEqualTo("선재 업고 튀어");
		assertThat(parsed.scenes()).hasSize(2);
		assertThat(parsed.scenes().get(0).imageUrls()).containsExactly("https://images.example.com/house.webp");
		assertThat(parsed.scenes().get(1).imageUrls())
				.containsExactly("https://images.example.com/bus-thumb.jpg", "https://images.example.com/bus-large.jpg");
	}

	private CrawlPostRow post(String url, String title) {
		return new CrawlPostRow(Hashing.sha256(url), url, title, null, null, null, null, null,
				IngestStatus.DISCOVERED, null, null);
	}

	private String fixture(String path) throws Exception {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}
}

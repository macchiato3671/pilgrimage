package com.moonback.pilgrimage.batch.ingest.geocode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoreanAddressNormalizerTest {

	private final KoreanAddressNormalizer normalizer = new KoreanAddressNormalizer();

	@Test
	void separatesStationExitAndWalkingNotes() {
		var normalized = normalizer.normalize("서울 강서구 마곡동 756-4 마곡나루역2번출구 도보7분");

		assertThat(normalized.address()).isEqualTo("서울 강서구 마곡동 756-4");
		assertThat(normalized.description()).contains("마곡나루역 2번 출구").contains("도보 7분");
	}

	@Test
	void prefersFirstSlashSeparatedAddressSegment() {
		var normalized = normalizer.normalize("부산 해운대구 우동 123-4 / 다른 지점 안내");

		assertThat(normalized.address()).isEqualTo("부산 해운대구 우동 123-4");
	}

	@Test
	void separatesClosedEstimatedAndCgNotes() {
		var normalized = normalizer.normalize("서울 종로구 계동 10 폐업 추정 CG");

		assertThat(normalized.address()).isEqualTo("서울 종로구 계동 10");
		assertThat(normalized.description()).contains("폐업").contains("추정").contains("CG");
	}
}

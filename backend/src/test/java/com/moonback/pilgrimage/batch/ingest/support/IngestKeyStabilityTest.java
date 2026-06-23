package com.moonback.pilgrimage.batch.ingest.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IngestKeyStabilityTest {

	@Test
	void sourceKeyDoesNotDependOnGlobalOrder() {
		String url = "https://ys-dl.tistory.com/123";
		byte[] first = Hashing.sha256(url + "\0" + "카페문" + "\0" + 1);
		byte[] afterInsertedEarlierScene = Hashing.sha256(url + "\0" + "카페문" + "\0" + 1);

		assertThat(Hashing.hex(first)).isEqualTo(Hashing.hex(afterInsertedEarlierScene));
	}

	@Test
	void ingestKeyUsesDramaNameAndCanonicalAddress() {
		byte[] first = Hashing.sha256(1399 + "\0" + "카페문" + "\0" + "서울 종로구 계동 10");
		byte[] second = Hashing.sha256(1399 + "\0" + "카페문" + "\0" + "서울 종로구 계동 10");
		byte[] differentAddress = Hashing.sha256(1399 + "\0" + "카페문" + "\0" + "서울 종로구 계동 11");

		assertThat(Hashing.hex(first)).isEqualTo(Hashing.hex(second));
		assertThat(Hashing.hex(first)).isNotEqualTo(Hashing.hex(differentAddress));
	}
}

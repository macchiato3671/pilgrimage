package com.ssafy.pilgrimage.batch.ingest.geocode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DomesticRegionPolicyTest {

	private final DomesticRegionPolicy policy = new DomesticRegionPolicy();

	@Test
	void acceptsSouthKoreanRegions() {
		List.of("서울", "부산광역시", "경기도", "강원특별자치도", "충청북도", "전북특별자치도",
				"전라남도", "경상북도", "제주특별자치도")
				.forEach(region -> assertThat(policy.isDomestic(region)).as(region).isTrue());
	}

	@Test
	void rejectsForeignRegions() {
		assertThat(policy.isDomestic("Tokyo")).isFalse();
		assertThat(policy.isDomestic("일본")).isFalse();
		assertThat(policy.isDomestic(null)).isFalse();
	}
}

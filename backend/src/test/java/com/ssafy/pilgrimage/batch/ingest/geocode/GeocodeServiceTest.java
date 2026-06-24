package com.ssafy.pilgrimage.batch.ingest.geocode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.batch.ingest.geocode.dto.KakaoAddress;
import com.ssafy.pilgrimage.batch.ingest.geocode.dto.KakaoDocument;
import com.ssafy.pilgrimage.batch.ingest.geocode.dto.KakaoSearchResponse;
import com.ssafy.pilgrimage.batch.ingest.model.IngestStatus;
import com.ssafy.pilgrimage.batch.ingest.persistence.PilgrimageIngestRepository;

class GeocodeServiceTest {

	private KakaoLocalClient kakaoLocalClient;
	private PilgrimageIngestRepository repository;
	private GeocodeService service;

	@BeforeEach
	void setUp() {
		kakaoLocalClient = mock(KakaoLocalClient.class);
		repository = mock(PilgrimageIngestRepository.class);
		when(repository.findFreshGeocodeCache(any())).thenReturn(Optional.empty());
		service = new GeocodeService(kakaoLocalClient, new DomesticRegionPolicy(), repository, new ObjectMapper());
	}

	@Test
	void exactFailureFallsBackToSimilarSuccess() {
		when(kakaoLocalClient.addressSearch("서울 강서구 마곡동 756-4", "exact"))
				.thenReturn(new KakaoSearchResponse(List.of()));
		when(kakaoLocalClient.addressSearch("서울 강서구 마곡동 756-4", "similar"))
				.thenReturn(new KakaoSearchResponse(List.of(document("서울 강서구 마곡동 756-4", "서울", "강서구"))));

		var outcome = service.geocode("서울 강서구 마곡동 756-4", "카페 문");

		assertThat(outcome.success()).isTrue();
		assertThat(outcome.result().latitude()).isEqualTo(37.5);
		assertThat(outcome.result().longitude()).isEqualTo(126.8);
	}

	@Test
	void foreignResultIsExcluded() {
		when(kakaoLocalClient.addressSearch(eq("Tokyo Shibuya 1"), any()))
				.thenReturn(new KakaoSearchResponse(List.of(document("Tokyo Shibuya 1", "Tokyo", "Shibuya"))));

		var outcome = service.geocode("Tokyo Shibuya 1", "해외 장면");

		assertThat(outcome.success()).isFalse();
		assertThat(outcome.status()).isEqualTo(IngestStatus.NON_DOMESTIC);
	}

	@Test
	void ambiguousCandidatesAreRejected() {
		when(kakaoLocalClient.addressSearch(eq("서울 어딘가"), any()))
				.thenReturn(new KakaoSearchResponse(List.of(
						document("서울 중구 세종대로 1", "서울", "중구"),
						document("서울 강남구 테헤란로 1", "서울", "강남구"))));

		var outcome = service.geocode("서울 어딘가", "카페");

		assertThat(outcome.success()).isFalse();
		assertThat(outcome.status()).isEqualTo(IngestStatus.GEOCODE_AMBIGUOUS);
	}

	private KakaoDocument document(String address, String region1, String region2) {
		return new KakaoDocument("장소", address, null, "126.8", "37.5",
				new KakaoAddress(address, region1, region2), null);
	}
}

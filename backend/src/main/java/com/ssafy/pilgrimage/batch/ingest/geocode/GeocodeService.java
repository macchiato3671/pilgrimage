package com.ssafy.pilgrimage.batch.ingest.geocode;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.batch.ingest.geocode.dto.KakaoDocument;
import com.ssafy.pilgrimage.batch.ingest.model.IngestStatus;
import com.ssafy.pilgrimage.batch.ingest.persistence.GeocodeCacheHit;
import com.ssafy.pilgrimage.batch.ingest.persistence.PilgrimageIngestRepository;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeocodeService {

	private final KakaoLocalClient kakaoLocalClient;
	private final DomesticRegionPolicy domesticRegionPolicy;
	private final PilgrimageIngestRepository repository;
	private final ObjectMapper objectMapper;

	public GeocodeOutcome geocode(String address, String sceneName) {
		String query = address == null ? "" : address.trim();
		byte[] queryHash = Hashing.sha256(query);
		Optional<GeocodeCacheHit> cache = repository.findFreshGeocodeCache(queryHash);
		if (cache.isPresent()) {
			if (cache.get().success()) {
				return domestic(cache.get().toResult());
			}
			return GeocodeOutcome.failure(IngestStatus.GEOCODE_NOT_FOUND, "GEOCODE_NOT_FOUND", "Cached no-result geocode");
		}

		var exact = kakaoLocalClient.addressSearch(query, "exact");
		Optional<GeocodeResult> exactResult = choose(exact.documents(), query, sceneName);
		if (exactResult.isPresent()) {
			repository.saveSuccessfulGeocodeCache(queryHash, query, exactResult.get());
			return domestic(exactResult.get());
		}
		if (isAmbiguous(exact.documents(), query, sceneName)) {
			return GeocodeOutcome.failure(IngestStatus.GEOCODE_AMBIGUOUS, "GEOCODE_AMBIGUOUS", "Ambiguous exact address candidates");
		}

		var similar = kakaoLocalClient.addressSearch(query, "similar");
		Optional<GeocodeResult> similarResult = choose(similar.documents(), query, sceneName);
		if (similarResult.isPresent()) {
			repository.saveSuccessfulGeocodeCache(queryHash, query, similarResult.get());
			return domestic(similarResult.get());
		}
		if (isAmbiguous(similar.documents(), query, sceneName)) {
			return GeocodeOutcome.failure(IngestStatus.GEOCODE_AMBIGUOUS, "GEOCODE_AMBIGUOUS", "Ambiguous similar address candidates");
		}

		String regionHint = firstRegionToken(query);
		var keyword = kakaoLocalClient.keywordSearch((sceneName == null ? "" : sceneName) + " " + regionHint);
		Optional<GeocodeResult> keywordResult = choose(keyword.documents(), query, sceneName);
		if (keywordResult.isPresent()) {
			repository.saveSuccessfulGeocodeCache(queryHash, query, keywordResult.get());
			return domestic(keywordResult.get());
		}
		if (isAmbiguous(keyword.documents(), query, sceneName)) {
			return GeocodeOutcome.failure(IngestStatus.GEOCODE_AMBIGUOUS, "GEOCODE_AMBIGUOUS", "Ambiguous keyword candidates");
		}

		repository.saveNoResultGeocodeCache(queryHash, query);
		return GeocodeOutcome.failure(IngestStatus.GEOCODE_NOT_FOUND, "GEOCODE_NOT_FOUND", "No Kakao geocode result");
	}

	private GeocodeOutcome domestic(GeocodeResult result) {
		if (result == null || Double.isNaN(result.latitude()) || Double.isNaN(result.longitude())) {
			return GeocodeOutcome.failure(IngestStatus.GEOCODE_NOT_FOUND, "GEOCODE_NOT_FOUND", "No coordinates returned");
		}
		if (!domesticRegionPolicy.isDomestic(result.region1Depth())) {
			return GeocodeOutcome.failure(IngestStatus.NON_DOMESTIC, "NON_DOMESTIC",
					"Geocode region is not South Korea: " + result.region1Depth());
		}
		return GeocodeOutcome.success(result);
	}

	private Optional<GeocodeResult> choose(List<KakaoDocument> documents, String address, String sceneName) {
		if (documents == null || documents.isEmpty()) {
			return Optional.empty();
		}
		List<ScoredDocument> scored = documents.stream()
				.map(document -> new ScoredDocument(document, score(document, address, sceneName)))
				.sorted(Comparator.comparingInt(ScoredDocument::score).reversed())
				.toList();
		ScoredDocument best = scored.getFirst();
		int next = scored.size() > 1 ? scored.get(1).score() : 0;
		if (documents.size() == 1 || (best.score() >= 50 && best.score() - next >= 10)) {
			return toResult(best.document());
		}
		return Optional.empty();
	}

	private boolean isAmbiguous(List<KakaoDocument> documents, String address, String sceneName) {
		return documents != null && documents.size() > 1 && choose(documents, address, sceneName).isEmpty();
	}

	private int score(KakaoDocument document, String address, String sceneName) {
		String canonical = nullToEmpty(document.canonicalAddress());
		String place = nullToEmpty(document.placeName());
		String compactAddress = compact(address);
		String compactCanonical = compact(canonical);
		int score = 0;
		if (!compactAddress.isBlank() && (compactCanonical.contains(compactAddress) || compactAddress.contains(compactCanonical))) {
			score += 70;
		}
		if (!firstRegionToken(address).isBlank() && canonical.startsWith(firstRegionToken(address))) {
			score += 15;
		}
		if (sceneName != null && !sceneName.isBlank() && compact(place).contains(compact(sceneName))) {
			score += 20;
		}
		return score;
	}

	private Optional<GeocodeResult> toResult(KakaoDocument document) {
		try {
			String canonical = document.canonicalAddress();
			if (canonical == null || canonical.isBlank() || document.x() == null || document.y() == null) {
				return Optional.empty();
			}
			double longitude = Double.parseDouble(document.x());
			double latitude = Double.parseDouble(document.y());
			return Optional.of(new GeocodeResult(canonical, document.region1Depth(), document.region2Depth(),
					latitude, longitude, objectMapper.writeValueAsString(document)));
		} catch (NumberFormatException | JsonProcessingException e) {
			return Optional.empty();
		}
	}

	private String firstRegionToken(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.trim().split("\\s+")[0];
	}

	private String compact(String value) {
		return nullToEmpty(value).replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private record ScoredDocument(KakaoDocument document, int score) {
	}

	@Builder
	public record GeocodeOutcome(boolean success, GeocodeResult result, IngestStatus status, String errorCode,
			String errorMessage) {
		public static GeocodeOutcome success(GeocodeResult result) {
			return new GeocodeOutcome(true, result, IngestStatus.SCENE_SAVED, null, null);
		}

		public static GeocodeOutcome failure(IngestStatus status, String errorCode, String errorMessage) {
			return new GeocodeOutcome(false, null, status, errorCode, errorMessage);
		}
	}
}

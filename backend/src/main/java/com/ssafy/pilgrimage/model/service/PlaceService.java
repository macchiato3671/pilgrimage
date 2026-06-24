package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.PlaceResponseDto;
import com.ssafy.pilgrimage.model.dto.response.PlaceSearchResponseDto;

public interface PlaceService {

	PlaceResponseDto getPlace(int placeId);

	PlaceSearchResponseDto searchPlace(
			String keyword, Integer contentTypeId, Double latitude, Double longitude,
			double radiusKm, int page, int size);

}

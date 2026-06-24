package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.response.PlaceResponseDto;

public interface PlaceService {

	PlaceResponseDto getPlace(int placeId);

}

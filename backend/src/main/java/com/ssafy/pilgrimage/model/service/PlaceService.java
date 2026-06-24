package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.PlaceResponseDto;

public interface PlaceService {

	PlaceResponseDto getPlace(int placeId);

}

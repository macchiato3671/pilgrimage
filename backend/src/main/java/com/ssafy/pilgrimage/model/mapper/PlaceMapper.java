package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.PlaceDto;
import com.ssafy.pilgrimage.model.dto.PlaceImageDto;

public interface PlaceMapper {

	List<PlaceImageDto> getImages(int placeId);

	PlaceDto getPlace(int placeId);

}

package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import com.moonback.pilgrimage.model.dto.PlaceDto;
import com.moonback.pilgrimage.model.dto.PlaceImageDto;

public interface PlaceMapper {

	List<PlaceImageDto> getImages(int placeId);

	PlaceDto getPlace(int placeId);

}

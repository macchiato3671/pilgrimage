package com.ssafy.pilgrimage.model.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.PlaceErrorCode;
import com.ssafy.pilgrimage.model.dto.ContentTypeDto;
import com.ssafy.pilgrimage.model.dto.PlaceDto;
import com.ssafy.pilgrimage.model.dto.PlaceImageDto;
import com.ssafy.pilgrimage.model.dto.response.PlaceResponseDto;
import com.ssafy.pilgrimage.model.mapper.PlaceMapper;
import com.ssafy.pilgrimage.model.service.PlaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {
	
	private final PlaceMapper placeMapper;
	
	@Override
	public PlaceResponseDto getPlace(int placeId) {

		PlaceDto place = placeMapper.getPlace(placeId);
		
		if (place == null) {
	        throw new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND);
	    }
		
		List<PlaceImageDto> images = placeMapper.getImages(placeId);
		
		return PlaceResponseDto.builder()
				.placeId(placeId)
				.contentId(place.getContentId())
				.name(place.getName())
				.address(place.getAddress())
				.latitude(place.getLatitude())
				.longitude(place.getLongitude())
				.description(place.getDescription())
				.contentType(
						ContentTypeDto.builder()
								.contentTypeId(place.getContentTypeId())
								.name(place.getContentTypeName())
								.build()
				)
				.images(images)
				.build();
	}

}

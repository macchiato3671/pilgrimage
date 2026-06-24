package com.moonback.pilgrimage.model.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.PlaceErrorCode;
import com.moonback.pilgrimage.model.dto.PlaceDto;
import com.moonback.pilgrimage.model.dto.PlaceImageDto;
import com.moonback.pilgrimage.model.dto.PlaceImageRowDto;
import com.moonback.pilgrimage.model.dto.response.PageResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlaceResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlaceSearchResponseDto;
import com.moonback.pilgrimage.model.mapper.PlaceMapper;
import com.moonback.pilgrimage.model.service.PlaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceServiceImpl implements PlaceService {

	private static final int MAX_PAGE_SIZE = 50;

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
				.contentTypeId(place.getContentTypeId())
				.contentTypeName(place.getContentTypeName())
				.images(images)
				.build();
	}

	@Override
	public PlaceSearchResponseDto searchPlace(String keyword, Integer contentTypeId, Double latitude, Double longitude,
			double radiusKm, int page, int size) {
		validateSearchRequest(radiusKm, page, size);

		int totalElements = placeMapper.countSearchPlace(keyword, contentTypeId, latitude, longitude, radiusKm);
		int totalPages = calculateTotalPages(totalElements, size);
		int offset = page * size;

		List<PlaceResponseDto> places = placeMapper.searchPlace(
				keyword,
				contentTypeId,
				latitude,
				longitude,
				radiusKm,
				size,
				offset
		);

		if (places.isEmpty()) {
			return PlaceSearchResponseDto.builder()
					.places(List.of())
					.page(createPageResponse(page, size, totalElements, totalPages))
					.build();
		}

		List<Integer> placeIds = places.stream()
				.map(PlaceResponseDto::getPlaceId)
				.toList();

		List<PlaceImageRowDto> imageRows = placeMapper.selectPlaceImagesByPlaceIds(placeIds);

		Map<Integer, List<PlaceImageDto>> imageMap = imageRows.stream()
				.collect(Collectors.groupingBy(
						PlaceImageRowDto::getPlaceId,
						Collectors.mapping(
								image -> PlaceImageDto.builder()
										.imgId(image.getImgId())
										.url(image.getUrl())
										.build(),
								Collectors.toList()
						)
				));

		List<PlaceResponseDto> placesWithImages = places.stream()
				.map(place -> PlaceResponseDto.builder()
						.placeId(place.getPlaceId())
						.contentId(place.getContentId())
						.contentTypeId(place.getContentTypeId())
						.contentTypeName(place.getContentTypeName())
						.name(place.getName())
						.address(place.getAddress())
						.latitude(place.getLatitude())
						.longitude(place.getLongitude())
						.description(place.getDescription())
						.images(imageMap.getOrDefault(place.getPlaceId(), List.of()))
						.build())
				.toList();

		return PlaceSearchResponseDto.builder()
				.places(placesWithImages)
				.page(createPageResponse(page, size, totalElements, totalPages))
				.build();
	}

	private void validateSearchRequest(double radiusKm, int page, int size) {
		if (radiusKm <= 0) {
			throw new BusinessException(PlaceErrorCode.INVALID_RADIUS_PARAMETER);
		}

		if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(PlaceErrorCode.INVALID_PAGE_REQUEST);
		}
	}

	private int calculateTotalPages(int totalElements, int size) {
		if (totalElements == 0) {
			return 0;
		}

		return (int) Math.ceil((double) totalElements / size);
	}

	private PageResponseDto createPageResponse(int page, int size, int totalElements, int totalPages) {
		return PageResponseDto.builder()
				.number(page)
				.size(size)
				.totalElements(totalElements)
				.totalPages(totalPages)
				.hasNext(page + 1 < totalPages)
				.hasPrevious(page > 0)
				.build();
	}

}

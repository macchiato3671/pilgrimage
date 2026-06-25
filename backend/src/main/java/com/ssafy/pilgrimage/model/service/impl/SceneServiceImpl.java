package com.ssafy.pilgrimage.model.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.SceneErrorCode;
import com.ssafy.pilgrimage.model.dto.NearPlaceDto;
import com.ssafy.pilgrimage.model.dto.PlaceImageDto;
import com.ssafy.pilgrimage.model.dto.PlaceImageRowDto;
import com.ssafy.pilgrimage.model.dto.SceneDto;
import com.ssafy.pilgrimage.model.dto.SceneImageDto;
import com.ssafy.pilgrimage.model.dto.response.NearPlaceResponseDto;
import com.ssafy.pilgrimage.model.dto.response.SceneResponseDto;
import com.ssafy.pilgrimage.model.mapper.PlaceMapper;
import com.ssafy.pilgrimage.model.mapper.SceneMapper;
import com.ssafy.pilgrimage.model.service.SceneService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SceneServiceImpl implements SceneService {
	
	private static final int MAX_PAGE_SIZE = 50;

	private final SceneMapper sceneMapper;
	private final PlaceMapper placeMapper;
	
	@Override
	@Transactional(readOnly = true)
	public SceneResponseDto getSceneDetail(int sceneId) {
		
		SceneDto scene = sceneMapper.findById(sceneId);
		
		if (scene == null) {
	        throw new BusinessException(SceneErrorCode.SCENE_NOT_FOUND);
	    }

        List<SceneImageDto> images = sceneMapper.findSceneImagesBySceneId(sceneId);

        return SceneResponseDto.builder()
                .sceneId(sceneId)
                .dramaId(scene.getDramaId())
                .name(scene.getName())
                .description(scene.getDescription())
                .address(scene.getAddress())
                .latitude(scene.getLatitude())
                .longitude(scene.getLongitude())
                .images(images)
                .build();
	}

	@Override
	@Transactional(readOnly = true)
	public NearPlaceResponseDto getNearPlace(int sceneId, int contentTypeId, double radiusKm, int page, int size) {
		validateNearPlaceRequest(contentTypeId, radiusKm, page, size);

		SceneDto scene = sceneMapper.findById(sceneId);

		if (scene == null) {
	        throw new BusinessException(SceneErrorCode.SCENE_NOT_FOUND);
	    }

		int totalElements = placeMapper.countNearPlaces(
				scene.getLatitude(),
				scene.getLongitude(),
				contentTypeId,
				radiusKm
		);
		int totalPages = calculateTotalPages(totalElements, size);
		int offset = page * size;

		List<NearPlaceDto> attractions = placeMapper.selectNearPlaces(
				scene.getLatitude(),
				scene.getLongitude(),
				contentTypeId,
				radiusKm,
				size,
				offset
		);

		attachImages(attractions);

		return NearPlaceResponseDto.builder()
				.sceneId(scene.getSceneId())
				.sceneName(scene.getName())
				.sceneLatitude(scene.getLatitude())
				.sceneLongitude(scene.getLongitude())
				.radiusKm(radiusKm)
				.attractions(attractions)
				.page(page)
				.size(size)
				.totalElements(totalElements)
				.totalPages(totalPages)
				.hasNext(page + 1 < totalPages)
				.build();
	}

	private void attachImages(List<NearPlaceDto> attractions) {
		if (attractions.isEmpty()) {
			return;
		}

		List<Integer> placeIds = attractions.stream()
				.map(NearPlaceDto::getPlaceId)
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

		attractions.forEach(attraction -> {
			attraction.setImages(imageMap.getOrDefault(attraction.getPlaceId(), List.of()));
		});
	}

	private void validateNearPlaceRequest(int contentTypeId, double radiusKm, int page, int size) {
		if (contentTypeId < 1) {
			throw new BusinessException(SceneErrorCode.INVALID_CONTENT_TYPE_ID);
		}

		if (radiusKm <= 0) {
			throw new BusinessException(SceneErrorCode.INVALID_RADIUS_PARAMETER);
		}

		if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(SceneErrorCode.INVALID_PAGE_PARAMETER);
		}
	}

	private int calculateTotalPages(int totalElements, int size) {
		if (totalElements == 0) {
			return 0;
		}

		return (int) Math.ceil((double) totalElements / size);
	}

}

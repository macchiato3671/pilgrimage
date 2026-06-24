package com.moonback.pilgrimage.model.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.SceneErrorCode;
import com.moonback.pilgrimage.model.dto.NearPlaceDto;
import com.moonback.pilgrimage.model.dto.SceneDto;
import com.moonback.pilgrimage.model.dto.SceneImageDto;
import com.moonback.pilgrimage.model.dto.response.NearPlaceResponseDto;
import com.moonback.pilgrimage.model.dto.response.SceneResponseDto;
import com.moonback.pilgrimage.model.mapper.PlaceMapper;
import com.moonback.pilgrimage.model.mapper.SceneMapper;
import com.moonback.pilgrimage.model.service.SceneService;

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

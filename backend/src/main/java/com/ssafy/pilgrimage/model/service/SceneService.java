package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.SceneResponseDto;
import com.ssafy.pilgrimage.model.dto.response.NearPlaceResponseDto;

public interface SceneService {

	SceneResponseDto getSceneDetail(int sceneId);

	NearPlaceResponseDto getNearPlace(int sceneId, int contentTypeId, double radiusKm, int page, int size);

}

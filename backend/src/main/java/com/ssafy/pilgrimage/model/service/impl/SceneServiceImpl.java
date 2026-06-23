package com.ssafy.pilgrimage.model.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.SceneErrorCode;
import com.ssafy.pilgrimage.model.dto.SceneDto;
import com.ssafy.pilgrimage.model.dto.SceneImageDto;
import com.ssafy.pilgrimage.model.dto.response.SceneResponseDto;
import com.ssafy.pilgrimage.model.mapper.SceneMapper;
import com.ssafy.pilgrimage.model.service.SceneService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SceneServiceImpl implements SceneService {
	
	private final SceneMapper sceneMapper;
	
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

}

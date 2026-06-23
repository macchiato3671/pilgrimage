package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import com.moonback.pilgrimage.model.dto.SceneDto;
import com.moonback.pilgrimage.model.dto.SceneImageDto;
import com.moonback.pilgrimage.model.dto.SceneImageRowDto;

public interface SceneMapper {

	SceneDto findById(int sceneId);

	List<SceneImageRowDto> selectSceneImagesBySceneIds(List<Integer> sceneIds);

	List<SceneImageDto> findSceneImagesBySceneId(int sceneId);

}

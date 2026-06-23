package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.SceneDto;
import com.ssafy.pilgrimage.model.dto.SceneImageDto;
import com.ssafy.pilgrimage.model.dto.SceneImageRowDto;

public interface SceneMapper {

	SceneDto findById(int sceneId);

	List<SceneImageRowDto> selectSceneImagesBySceneIds(List<Integer> sceneIds);

	List<SceneImageDto> findSceneImagesBySceneId(int sceneId);

}

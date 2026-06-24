package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.moonback.pilgrimage.model.dto.SceneDto;
import com.moonback.pilgrimage.model.dto.SceneImageDto;
import com.moonback.pilgrimage.model.dto.SceneImageRowDto;

public interface SceneMapper {

	SceneDto findById(int sceneId);

	List<SceneImageRowDto> selectSceneImagesBySceneIds(@Param("sceneIds") List<Integer> sceneIds);

	int countSceneByDramaId(int dramaId);
	
	List<SceneDto> getSceneByDramaId(@Param("dramaId") int dramaId, @Param("size") int size, @Param("offset") int offset);

	List<SceneImageDto> findSceneImagesBySceneId(int sceneId);

}

package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.moonback.pilgrimage.model.dto.DramaImageRowDto;

public interface DramaMapper {

	boolean existsByDramaId(int dramaId);
	
	List<DramaImageRowDto> selectDramaImagesByDramaIds(@Param("dramaIds") List<Integer> dramaIds);

}

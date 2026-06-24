package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ssafy.pilgrimage.model.dto.NearPlaceDto;

public interface PlaceMapper {

	int countNearPlaces(
			@Param("latitude") double latitude,
			@Param("longitude") double longitude,
			@Param("contentTypeId") int contentTypeId,
			@Param("radiusKm") double radiusKm
	);

	List<NearPlaceDto> selectNearPlaces(
			@Param("latitude") double latitude,
			@Param("longitude") double longitude,
			@Param("contentTypeId") int contentTypeId,
			@Param("radiusKm") double radiusKm,
			@Param("size") int size,
			@Param("offset") int offset
	);
}

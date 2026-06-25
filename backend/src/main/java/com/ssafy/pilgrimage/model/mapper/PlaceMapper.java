package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ssafy.pilgrimage.model.dto.NearPlaceDto;
import com.ssafy.pilgrimage.model.dto.PlaceDto;
import com.ssafy.pilgrimage.model.dto.PlaceImageDto;
import com.ssafy.pilgrimage.model.dto.PlaceImageRowDto;
import com.ssafy.pilgrimage.model.dto.response.PlaceResponseDto;

public interface PlaceMapper {

	List<PlaceImageDto> getImages(int placeId);

	List<PlaceImageRowDto> selectPlaceImagesByPlaceIds(@Param("placeIds") List<Integer> placeIds);

	PlaceDto getPlace(int placeId);

	int countSearchPlace(
			@Param("keyword") String keyword,
			@Param("contentTypeId") Integer contentTypeId,
			@Param("latitude") Double latitude,
			@Param("longitude") Double longitude,
			@Param("radiusKm") double radiusKm
	);

	List<PlaceResponseDto> searchPlace(
			@Param("keyword") String keyword,
			@Param("contentTypeId") Integer contentTypeId,
			@Param("latitude") Double latitude,
			@Param("longitude") Double longitude,
			@Param("radiusKm") double radiusKm,
			@Param("size") int size,
			@Param("offset") int offset
	);

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

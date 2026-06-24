package com.moonback.pilgrimage.model.dto.response;

import java.util.List;

import com.moonback.pilgrimage.model.dto.NearPlaceDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearPlaceResponseDto {
	private int sceneId;
	private String sceneName;
	private double sceneLatitude;
	private double sceneLongitude;
	private double radiusKm;
	private List<NearPlaceDto> attractions;
	private int page;
	private int size;
	private int totalElements;
	private int totalPages;
	private Boolean hasNext;
}

package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.PlaceImageDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceResponseDto {
	
	private int placeId;
	private int contentId;
	private int contentTypeId;
	private String contentTypeName;
	private String name;
	private String address;
	private double latitude;
	private double longitude;
	private String description;
	
	List<PlaceImageDto> images;
}

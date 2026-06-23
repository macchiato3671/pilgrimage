package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.SceneImageDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SceneResponseDto {
	
	private int sceneId;
	private int dramaId;
	private String name;
	private String description;
	private String address;
    private double latitude;
    private double longitude;
    
	List<SceneImageDto> images;
}

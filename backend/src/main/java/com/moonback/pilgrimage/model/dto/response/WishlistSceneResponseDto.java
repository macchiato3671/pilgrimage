package com.moonback.pilgrimage.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.moonback.pilgrimage.model.dto.SceneImageDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistSceneResponseDto {
	private int wishlistId;
	private int sceneId;
	private String name;
	private String description;
	private String address;
	private double latitude;
	private double longitude;
	private LocalDateTime createdAt;
	
	private List<SceneImageDto> images;
}

package com.ssafy.pilgrimage.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistSceneRowDto {
	private int wishlistId;
	private int sceneId;
	private String name;
	private String description;
	private String address;
	private double latitude;
	private double longitude;
	private LocalDateTime createdAt;
}

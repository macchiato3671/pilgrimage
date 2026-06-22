package com.moonback.pilgrimage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SceneDto {
	private int sceneId;
	private int dramaId;
	private String name;
	private String description;
	private String address;
	private double latitude;
	private double longitude;
	private String img_url;
}

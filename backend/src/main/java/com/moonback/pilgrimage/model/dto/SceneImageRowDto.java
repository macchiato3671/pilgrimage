package com.moonback.pilgrimage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SceneImageRowDto {
	private int sceneId;
	private int imgId;
	private String url;
}

package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DramaSceneResponseDto {
	private int dramaId;
	private String title;
	private List<SceneResponseDto> scenes;
	private PageResponseDto page;
}

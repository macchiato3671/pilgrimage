package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WishlistScenePageResponseDto {
	List<WishlistSceneResponseDto> scenes;
	private PageResponseDto page;
}

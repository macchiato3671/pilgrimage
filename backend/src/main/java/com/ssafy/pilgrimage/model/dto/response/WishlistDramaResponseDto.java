package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class WishlistDramaResponseDto {
	List<DramaResponseDto> dramas;
}

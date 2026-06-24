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
public class KeywordDramaResponseDto {
	private String keyword;
	private List<DramaResponseDto> dramas;
	private PageResponseDto page;
}

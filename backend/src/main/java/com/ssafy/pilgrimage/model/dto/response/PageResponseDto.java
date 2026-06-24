package com.ssafy.pilgrimage.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PageResponseDto {
	private int number;
	private int size;
	private int totalElements;
	private int totalPages;
	private Boolean hasNext;
	private Boolean hasPrevious;
}

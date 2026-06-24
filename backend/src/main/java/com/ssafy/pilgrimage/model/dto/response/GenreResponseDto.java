package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.GenreDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenreResponseDto {
	private List<GenreDto> genres;
}

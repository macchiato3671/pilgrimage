package com.moonback.pilgrimage.model.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.moonback.pilgrimage.model.dto.GenreDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class DramaResponseDto {
	private int dramaId;
	private String title;
	private LocalDate releasedAt;
	private String description;
	private String url;
	private List<GenreDto> genres;
}

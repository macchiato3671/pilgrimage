package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.response.DramaSceneResponseDto;
import com.moonback.pilgrimage.model.dto.response.GenreDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.GenreResponseDto;
import com.moonback.pilgrimage.model.dto.response.KeywordDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearResponseDto;

public interface DramaService {
	DramaSceneResponseDto getScene(int dramaId, int page, int size);
	
	YearResponseDto getYears();

	YearDramaResponseDto getYearDrama(int year, int page, int size);
	
	GenreResponseDto getGenres();
	
	GenreDramaResponseDto getGenreDrama(int genreId, int page, int size);

	KeywordDramaResponseDto searchDrama(String keyword, int page, int size);

}

package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.DramaSceneResponseDto;
import com.ssafy.pilgrimage.model.dto.response.GenreDramaResponseDto;
import com.ssafy.pilgrimage.model.dto.response.GenreResponseDto;
import com.ssafy.pilgrimage.model.dto.response.KeywordDramaResponseDto;
import com.ssafy.pilgrimage.model.dto.response.YearDramaResponseDto;
import com.ssafy.pilgrimage.model.dto.response.YearResponseDto;

public interface DramaService {
	DramaSceneResponseDto getScene(int dramaId, int page, int size);
	
	YearResponseDto getYears();

	YearDramaResponseDto getYearDrama(int year, int page, int size);
	
	GenreResponseDto getGenres();
	
	GenreDramaResponseDto getGenreDrama(int genreId, int page, int size);

	KeywordDramaResponseDto searchDrama(String keyword, int page, int size);

}

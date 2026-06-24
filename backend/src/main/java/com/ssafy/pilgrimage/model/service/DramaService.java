package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.GenreResponseDto;
import com.ssafy.pilgrimage.model.dto.response.YearResponseDto;

public interface DramaService {

	YearResponseDto getYears();

	GenreResponseDto getGenres();

}

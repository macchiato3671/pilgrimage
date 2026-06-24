package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.response.GenreResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearResponseDto;

public interface DramaService {

	YearResponseDto getYears();

	GenreResponseDto getGenres();

}

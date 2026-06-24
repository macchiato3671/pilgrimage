package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ssafy.pilgrimage.model.dto.DramaDto;
import com.ssafy.pilgrimage.model.dto.DramaGenreRowDto;
import com.ssafy.pilgrimage.model.dto.DramaImageRowDto;
import com.ssafy.pilgrimage.model.dto.GenreDto;
import com.ssafy.pilgrimage.model.dto.response.GenreResponseDto;
import com.ssafy.pilgrimage.model.dto.response.YearResponseDto;

public interface DramaMapper {

	boolean existsByDramaId(int dramaId);
	
	DramaDto findByDramaId(int dramaId);
	
	List<DramaImageRowDto> selectDramaImagesByDramaIds(@Param("dramaIds") List<Integer> dramaIds);

	List<Integer> getYears();

	List<GenreDto> getGenres();
	
	GenreDto findGenreById(int genreId);

	int countDramasByYear(int year);

	List<DramaDto> selectDramasByYear(@Param("year") int year, @Param("size") int size, @Param("offset") int offset);

	List<DramaGenreRowDto> selectGenresByDramaIds(@Param("dramaIds") List<Integer> dramaIds);

	int countDramasByGenre(int genreId);

	List<DramaDto> selectDramasByGenre(@Param("genreId") int genreId, @Param("size") int size, @Param("offset") int offset);

	int countDramasByKeyword(String keyword);

	List<DramaDto> selectDramasByKeyword(@Param("keyword") String keyword, @Param("size") int size, @Param("offset") int offset);

}

package com.moonback.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.model.dto.response.DramaSceneResponseDto;
import com.moonback.pilgrimage.model.dto.response.GenreDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.GenreResponseDto;
import com.moonback.pilgrimage.model.dto.response.KeywordDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearDramaResponseDto;
import com.moonback.pilgrimage.model.dto.response.YearResponseDto;
import com.moonback.pilgrimage.model.service.DramaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dramas")
@RequiredArgsConstructor
public class DramaController {
	
	private final DramaService dramaService;
	
	@GetMapping("/{dramaId}")
	public ResponseEntity<DramaSceneResponseDto> getDramaScenes(@PathVariable int dramaId,
											@RequestParam(defaultValue = "0") int page,
											@RequestParam(defaultValue = "10") int size){
		DramaSceneResponseDto response = dramaService.getScene(dramaId, page, size);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/years")
	public ResponseEntity<YearResponseDto> getYears(){
		YearResponseDto response = dramaService.getYears();
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/years/{year}")
	public ResponseEntity<YearDramaResponseDto> getYearDrama(@PathVariable int year,
											@RequestParam(defaultValue = "0") int page,
											@RequestParam(defaultValue = "10") int size){
		YearDramaResponseDto response = dramaService.getYearDrama(year, page, size);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/genres")
	public ResponseEntity<GenreResponseDto> getGenres(){
		GenreResponseDto response = dramaService.getGenres();
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/genres/{genreId}")
	public ResponseEntity<GenreDramaResponseDto> getGenreDrama(@PathVariable int genreId,
											@RequestParam(defaultValue = "0") int page,
											@RequestParam(defaultValue = "10") int size){
		GenreDramaResponseDto response = dramaService.getGenreDrama(genreId, page, size);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/search")
	public ResponseEntity<KeywordDramaResponseDto> searchDrama(@RequestParam String keyword,
											@RequestParam(defaultValue = "0") int page,
											@RequestParam(defaultValue = "10") int size){
		KeywordDramaResponseDto response = dramaService.searchDrama(keyword, page, size);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
}

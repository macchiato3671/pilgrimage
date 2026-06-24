package com.ssafy.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pilgrimage.model.dto.response.PlaceResponseDto;
import com.ssafy.pilgrimage.model.service.PlaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {
	
	private final PlaceService placeService;
	
	@GetMapping("/{placeId}")
	public ResponseEntity<PlaceResponseDto> getPlace(@PathVariable int placeId){
		
		PlaceResponseDto response = placeService.getPlace(placeId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
}

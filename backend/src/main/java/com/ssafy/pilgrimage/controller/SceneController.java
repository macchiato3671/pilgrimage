package com.ssafy.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pilgrimage.model.dto.response.SceneResponseDto;
import com.ssafy.pilgrimage.model.service.SceneService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
public class SceneController {
	
	private final SceneService sceneService;
	
	@GetMapping("/{sceneId}")
	public ResponseEntity<SceneResponseDto> getSceneDetail(@PathVariable int sceneId){
		
		SceneResponseDto response = sceneService.getSceneDetail(sceneId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@GetMapping("/{sceneId}/nearby-attractions")
	public ResponseEntity<> getNearPlace(@PathVariable int sceneId,
										@RequestParam(defaultValue = "12") int contentTypeId,
										@RequestParam(defaultValue = "3.0") double radiusKm,
										@RequestParam(defaultValue = "0") int page,
										@RequestParam(defaultValue = "10") int size){
		response = sceneService.getNearPlace(int sceneId, 
											int contentTypeId, 
											double radius, 
											int page, 
											int size);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
}

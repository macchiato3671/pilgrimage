package com.ssafy.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}

package com.moonback.pilgrimage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.model.dto.request.LoginRequestDto;
import com.moonback.pilgrimage.model.dto.response.LoginResponseDto;
import com.moonback.pilgrimage.model.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthService authService;
	
	@PostMapping("/login")
	public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request){
		LoginResponseDto response = authService.login(request);

        return ResponseEntity.ok(response);
	}
}

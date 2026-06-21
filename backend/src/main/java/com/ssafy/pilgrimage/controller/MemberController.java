package com.ssafy.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pilgrimage.model.dto.request.SignupRequestDto;
import com.ssafy.pilgrimage.model.dto.response.MemberResponseDto;
import com.ssafy.pilgrimage.model.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService memberService;
	
	@PostMapping("/members")
	public ResponseEntity<MemberResponseDto> signup(@RequestBody SignupRequestDto request) {
		MemberResponseDto response = memberService.signup(request);
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(response);
	}
}

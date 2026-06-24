package com.moonback.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.model.dto.request.PatchRequestDto;
import com.moonback.pilgrimage.model.dto.request.SignupRequestDto;
import com.moonback.pilgrimage.model.dto.request.WithdrawRequestDto;
import com.moonback.pilgrimage.model.dto.response.MemberResponseDto;
import com.moonback.pilgrimage.model.service.MemberService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;


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
	
	@GetMapping("/me")
	public ResponseEntity<MemberResponseDto> getMyInfo(){
		int memberId = getAuthenticatedMemberId();
		
		MemberResponseDto response = memberService.getMyInfo(memberId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@PatchMapping("/me")
	public ResponseEntity<MemberResponseDto> patchMyInfo(@RequestBody PatchRequestDto request){
		int memberId = getAuthenticatedMemberId();
		
		MemberResponseDto response = memberService.patchMyInfo(request, memberId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(response);
	}
	
	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw(@RequestBody WithdrawRequestDto request){
		int memberId = getAuthenticatedMemberId();
		
		memberService.withdraw(request, memberId);
		
		return ResponseEntity
				.status(HttpStatus.NO_CONTENT)
				.build();
	}
	
	private int getAuthenticatedMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Integer.parseInt(authentication.getName());
	}
}

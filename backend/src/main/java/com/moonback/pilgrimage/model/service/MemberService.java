package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.request.PatchRequestDto;
import com.moonback.pilgrimage.model.dto.request.SignupRequestDto;
import com.moonback.pilgrimage.model.dto.request.WithdrawRequestDto;
import com.moonback.pilgrimage.model.dto.response.MemberResponseDto;

public interface MemberService {
	MemberResponseDto signup(SignupRequestDto request);

	MemberResponseDto getMyInfo(int memberId);

	MemberResponseDto patchMyInfo(PatchRequestDto request, int memberId);
	
	void withdraw(WithdrawRequestDto request, int memberId);
}

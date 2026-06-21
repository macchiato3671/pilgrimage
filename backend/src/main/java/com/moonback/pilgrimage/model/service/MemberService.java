package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.request.SignupRequestDto;
import com.moonback.pilgrimage.model.dto.response.MemberResponseDto;

public interface MemberService {
	MemberResponseDto signup(SignupRequestDto request);
}

package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.request.SignupRequestDto;
import com.ssafy.pilgrimage.model.dto.response.MemberResponseDto;

public interface MemberService {
	MemberResponseDto signup(SignupRequestDto request);
}

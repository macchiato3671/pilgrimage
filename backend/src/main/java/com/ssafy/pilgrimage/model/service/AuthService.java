package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.request.LoginRequestDto;
import com.ssafy.pilgrimage.model.dto.response.LoginResponseDto;

public interface AuthService {
	LoginResponseDto login(LoginRequestDto request);
}

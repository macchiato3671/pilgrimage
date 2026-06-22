package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.request.LoginRequestDto;
import com.moonback.pilgrimage.model.dto.response.LoginResponseDto;

public interface AuthService {
	LoginResponseDto login(LoginRequestDto request);
}

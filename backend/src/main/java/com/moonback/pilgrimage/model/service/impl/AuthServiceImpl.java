package com.moonback.pilgrimage.model.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.AuthErrorCode;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.dto.request.LoginRequestDto;
import com.moonback.pilgrimage.model.dto.response.LoginResponseDto;
import com.moonback.pilgrimage.model.mapper.MemberMapper;
import com.moonback.pilgrimage.model.service.AuthService;
import com.moonback.pilgrimage.model.type.MemberStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	
	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;
	
	@Override
	public LoginResponseDto login(LoginRequestDto request) {
		MemberDto member = memberMapper.findByEmail(request.getEmail());
		
		if(member == null) {
			throw new BusinessException(AuthErrorCode.MEMBER_ACCESS_DENIED);
		}
		
		if(!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		
		if (!member.getStatusId().equals(MemberStatus.ACTIVE.getId())) {
			throw new BusinessException(AuthErrorCode.MEMBER_ACCESS_DENIED);
        }
		
		String accessToken = "temporary-access-token";
        String refreshToken = "temporary-refresh-token";
        Integer expiresIn = 3600;
		
        return LoginResponseDto.of(accessToken, refreshToken, expiresIn, member);
	}

}

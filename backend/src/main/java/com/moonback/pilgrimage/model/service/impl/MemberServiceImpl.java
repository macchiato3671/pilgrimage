package com.moonback.pilgrimage.model.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.MemberErrorCode;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.dto.request.SignupRequestDto;
import com.moonback.pilgrimage.model.dto.response.MemberResponseDto;
import com.moonback.pilgrimage.model.mapper.MemberMapper;
import com.moonback.pilgrimage.model.service.MemberService;
import com.moonback.pilgrimage.model.type.MemberRole;
import com.moonback.pilgrimage.model.type.MemberStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
	
	private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    
	@Override
	public MemberResponseDto signup(SignupRequestDto request) {
		if (memberMapper.countByEmail(request.getEmail()) > 0) {
            throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }
		
		MemberDto member = new MemberDto();
		member.setEmail(request.getEmail());
		member.setPassword(passwordEncoder.encode(request.getPassword()));
		member.setNickname(request.getNickname());
		member.setRoleId(MemberRole.USER.getId());
		member.setStatusId(MemberStatus.ACTIVE.getId());
		
		memberMapper.insertMember(member);
		
		MemberDto savedMember = memberMapper.findById(member.getMemberId());

        return MemberResponseDto.from(savedMember);
	}

}

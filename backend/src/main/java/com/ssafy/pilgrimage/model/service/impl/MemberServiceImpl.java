package com.ssafy.pilgrimage.model.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.MemberErrorCode;
import com.ssafy.pilgrimage.model.dto.MemberDto;
import com.ssafy.pilgrimage.model.dto.request.SignupRequestDto;
import com.ssafy.pilgrimage.model.dto.response.MemberResponseDto;
import com.ssafy.pilgrimage.model.mapper.MemberMapper;
import com.ssafy.pilgrimage.model.service.MemberService;
import com.ssafy.pilgrimage.model.type.MemberRole;
import com.ssafy.pilgrimage.model.type.MemberStatus;

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

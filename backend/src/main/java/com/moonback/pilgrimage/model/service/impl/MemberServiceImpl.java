package com.moonback.pilgrimage.model.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.MemberErrorCode;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.dto.request.PatchRequestDto;
import com.moonback.pilgrimage.model.dto.request.SignupRequestDto;
import com.moonback.pilgrimage.model.dto.request.WithdrawRequestDto;
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
	@Transactional
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

	@Override
	@Transactional(readOnly = true)
	public MemberResponseDto getMyInfo(int memberId) {
		MemberDto member = memberMapper.findById(memberId);
		
		if(member == null) {
			throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
		
		if(memberId != member.getMemberId()) {
			throw new BusinessException(MemberErrorCode.MEMBER_ACCESS_DENIED);
		}
		
		if(member.getStatusId() == 2) {
			throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN);
		}
		else if(member.getStatusId() == 3) {
			throw new BusinessException(MemberErrorCode.MEMBER_SUSPENDED);
		}
		
		return MemberResponseDto.from(member);
	}

	@Override
	@Transactional
	public MemberResponseDto patchMyInfo(PatchRequestDto request, int memberId) {
		MemberDto member = memberMapper.findById(memberId);
		
		if(member == null) {
			throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
		
		if(memberId != member.getMemberId()) {
			throw new BusinessException(MemberErrorCode.MEMBER_ACCESS_DENIED);
		}
		
		if(!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
			throw new BusinessException(MemberErrorCode.INVALID_PASSWORD);
		}
		
		if(member.getStatusId() == 2) {
			throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN);
		}
		else if(member.getStatusId() == 3) {
			throw new BusinessException(MemberErrorCode.MEMBER_SUSPENDED);
		}
		
		if(request.getCurrentPassword() == null && request.getNewPassword() == null &&
			request.getEmail() == null && request.getNickname() == null) {
			throw new BusinessException(MemberErrorCode.EMPTY_UPDATE_FILEDS);
		}
		
		if (memberMapper.countByEmail(request.getEmail()) > 0) {
            throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }
		
		memberMapper.updateMember(memberId, request.getEmail(), request.getNickname(), passwordEncoder.encode(request.getNewPassword()));
		
		MemberDto updatedMember = memberMapper.findById(memberId);
		
		return MemberResponseDto.from(updatedMember);
	}

	@Override
	@Transactional
	public void withdraw(WithdrawRequestDto request, int memberId) {
		MemberDto member = memberMapper.findById(memberId);
		
		if(member == null) {
			throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
		}
		
		if(memberId != member.getMemberId()) {
			throw new BusinessException(MemberErrorCode.MEMBER_ACCESS_DENIED);
		}
		
		if(!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			throw new BusinessException(MemberErrorCode.INVALID_PASSWORD);
		}
		
		if(member.getStatusId() == 2) {
			throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN);
		}
		else if(member.getStatusId() == 3) {
			throw new BusinessException(MemberErrorCode.MEMBER_SUSPENDED);
		}
		
		memberMapper.withdrawById(memberId);
	}

}

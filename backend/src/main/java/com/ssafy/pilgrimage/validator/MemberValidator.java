package com.ssafy.pilgrimage.validator;

import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.MemberErrorCode;
import com.ssafy.pilgrimage.model.dto.MemberDto;
import com.ssafy.pilgrimage.model.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberValidator {
	
	private final MemberMapper memberMapper;

    public MemberDto validateActiveMember(int memberId) {
        MemberDto member = memberMapper.findById(memberId);

        if (member == null) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        if (member.getStatusId() == 2) {
            throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }

        if (member.getStatusId() == 3) {
            throw new BusinessException(MemberErrorCode.MEMBER_SUSPENDED);
        }

        return member;
    }
}

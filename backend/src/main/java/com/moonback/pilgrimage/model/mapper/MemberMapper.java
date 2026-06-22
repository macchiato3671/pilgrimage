package com.moonback.pilgrimage.model.mapper;

import com.moonback.pilgrimage.model.dto.MemberDto;

public interface MemberMapper {
	MemberDto findByEmail(String email);
	int insertMember(MemberDto member);
	int countByEmail(String email);
	MemberDto findById(Integer memberId);
}

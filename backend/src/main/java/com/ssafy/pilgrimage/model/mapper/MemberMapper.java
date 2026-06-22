package com.ssafy.pilgrimage.model.mapper;

import com.ssafy.pilgrimage.model.dto.MemberDto;

public interface MemberMapper {
	MemberDto findByEmail(String email);
	int insertMember(MemberDto member);
	int countByEmail(String email);
	MemberDto findById(Integer memberId);
	int withdrawById(Integer memberId);
	int updateMember(int memberId, String email, String nickname, String password);
}

package com.ssafy.pilgrimage.model.dto.response;

import java.time.LocalDateTime;

import com.ssafy.pilgrimage.model.dto.MemberDto;
import com.ssafy.pilgrimage.model.type.MemberRole;
import com.ssafy.pilgrimage.model.type.MemberStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponseDto {
	
	private Integer memberId;
    private String email;
    private LocalDateTime createdAt;
    private String nickname;
    private String role;
    private String status;

    public static MemberResponseDto from(MemberDto member) {
        return MemberResponseDto.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .createdAt(member.getCreatedAt())
                .nickname(member.getNickname())
                .role(MemberRole.fromId(member.getRoleId()).name())
                .status(MemberStatus.fromId(member.getStatusId()).name())
                .build();
    }
}

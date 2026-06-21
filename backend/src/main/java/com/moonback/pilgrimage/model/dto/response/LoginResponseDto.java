package com.moonback.pilgrimage.model.dto.response;

import com.moonback.pilgrimage.model.dto.MemberDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {
	
	private String tokenType;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private MemberResponseDto member;
    
    public static LoginResponseDto of(
            String accessToken,
            String refreshToken,
            Integer expiresIn,
            MemberDto member
    ) {
        return LoginResponseDto.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .member(MemberResponseDto.from(member))
                .build();
    }
}

package com.ssafy.pilgrimage.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
	
	private Integer memberId;
	private String email;
	private String password;
	private String nickname;
	private Integer roleId;
	private Integer statusId;
	private LocalDateTime createdAt;
}

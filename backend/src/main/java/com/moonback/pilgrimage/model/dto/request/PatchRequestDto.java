package com.moonback.pilgrimage.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatchRequestDto {
	private String email;
	private String nickname;
	private String currentPassword;
	private String newPassword;
}

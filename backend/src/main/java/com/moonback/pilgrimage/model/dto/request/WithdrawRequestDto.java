package com.moonback.pilgrimage.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WithdrawRequestDto {
	private String password;
	private String reason;
}

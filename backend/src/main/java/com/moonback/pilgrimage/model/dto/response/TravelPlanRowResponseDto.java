package com.moonback.pilgrimage.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanRowResponseDto {
	private Integer planId;
	private Integer memberId;
	private String title;
	private String createdAt;
	private String updatedAt;
	private String beginDate;
	private String endDate;
	private String memo;
}

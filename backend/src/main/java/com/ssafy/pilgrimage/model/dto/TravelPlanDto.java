package com.ssafy.pilgrimage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanDto {
	private Integer planId;
	private String title;
	private String createdAt;
	private String updatedAt;
	private String beginDate;
	private String endDate;
}

package com.ssafy.pilgrimage.model.dto.response;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.TravelPlanDetailDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponseDto {
	private Integer planId;
	private String title;
	private String createdAt;
	private String updatedAt;
	private String beginDate;
	private String endDate;
	private String memo;
	private List<TravelPlanDetailDto> details;
}

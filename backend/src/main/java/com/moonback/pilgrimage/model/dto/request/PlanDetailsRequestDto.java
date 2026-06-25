package com.moonback.pilgrimage.model.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDetailsRequestDto {
	private List<TravelPlanDetailRequestDto> details;
}

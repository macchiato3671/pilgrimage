package com.moonback.pilgrimage.model.dto.response;

import java.util.List;

import com.moonback.pilgrimage.model.dto.TravelPlanDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlansResponseDto {
	private List<TravelPlanDto> travelPlans;
}

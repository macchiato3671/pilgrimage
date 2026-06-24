package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;

public interface PlanService {
	PlansResponseDto getPlans(
			final int memeberId,
			final int page,
			final int pageSize
	);
	void addPlan(
			final TravelPlanRowDto travelPlanRow
	);

}

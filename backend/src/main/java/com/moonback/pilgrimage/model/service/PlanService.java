package com.moonback.pilgrimage.model.service;

import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;
import com.moonback.pilgrimage.model.dto.response.PlanResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;

public interface PlanService {
	PlansResponseDto getPlans(
			final int memeberId,
			final int page,
			final int pageSize
	);
	PlanResponseDto getPlan(
			final int memberId,
			final int planId
	);
	void addPlan(
			final TravelPlanRowDto travelPlanRow
	);

}

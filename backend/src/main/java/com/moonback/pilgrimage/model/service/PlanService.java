package com.moonback.pilgrimage.model.service;

import java.util.List;

import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;
import com.moonback.pilgrimage.model.dto.request.TravelPlanDetailRequestDto;
import com.moonback.pilgrimage.model.dto.response.PlanDetailsResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlanResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;
import com.moonback.pilgrimage.model.dto.response.TravelPlanRowResponseDto;

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
	TravelPlanRowResponseDto updatePlan(
			final int memberId,
			final int planId,
			final TravelPlanRowDto travelPlanRow
	);
	void deletePlan(
			final int memberId,
			final int planId
	);
	PlanDetailsResponseDto updatePlanDetails(
			final int memberId,
			final int planId,
			final List<TravelPlanDetailRequestDto> planDetails
	);
	PlanResponseDto addPlan(
			final TravelPlanRowDto travelPlanRow
	);

}

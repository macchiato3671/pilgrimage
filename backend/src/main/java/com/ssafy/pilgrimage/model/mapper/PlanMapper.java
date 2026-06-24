package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.TravelPlanDetailRowDto;
import com.ssafy.pilgrimage.model.dto.TravelPlanRowDto;

public interface PlanMapper {
	List<TravelPlanRowDto> selectPlans(
			final int memberId,
			final int offset,
			final int pageSize
			);
	TravelPlanRowDto selectPlan(
			final int memberId,
			final int planId
			);
	List<TravelPlanDetailRowDto> selectPlanDetails(
			final int planId
			);
	void insertPlan(
			final TravelPlanRowDto dto
			);

}

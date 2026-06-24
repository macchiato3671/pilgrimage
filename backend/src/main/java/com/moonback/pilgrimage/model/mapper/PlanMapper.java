package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;

public interface PlanMapper {
	List<TravelPlanRowDto> selectPlans(
			final int memberId,
			final int offset,
			final int pageSize
			);
	void insertPlan(
			final TravelPlanRowDto dto
			);

}

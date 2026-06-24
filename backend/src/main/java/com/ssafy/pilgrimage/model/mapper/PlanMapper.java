package com.ssafy.pilgrimage.model.mapper;

import java.util.List;

import com.ssafy.pilgrimage.model.dto.TravelPlanRowDto;

public interface PlanMapper {
	List<TravelPlanRowDto> findPlans(
			final int memberId,
			final int offset,
			final int pageSize
			);
}

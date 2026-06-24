package com.ssafy.pilgrimage.model.service;

import com.ssafy.pilgrimage.model.dto.response.PlansResponseDto;

public interface PlanService {
	PlansResponseDto getPlans(
			final int memeberId,
			final int page,
			final int pageSize
	);
}

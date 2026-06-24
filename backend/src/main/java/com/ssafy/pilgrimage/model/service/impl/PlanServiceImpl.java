package com.ssafy.pilgrimage.model.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.pilgrimage.model.dto.TravelPlanDto;
import com.ssafy.pilgrimage.model.dto.TravelPlanRowDto;
import com.ssafy.pilgrimage.model.dto.response.PlansResponseDto;
import com.ssafy.pilgrimage.model.mapper.PlanMapper;
import com.ssafy.pilgrimage.model.service.PlanService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {
	private final PlanMapper mapper;

	@Override
	public PlansResponseDto getPlans(
			final int memberId,
			final int page,
			final int pageSize
			) {

		List<TravelPlanRowDto> planRows = mapper.selectPlans(
				memberId,
				(page - 1) * pageSize,
				pageSize
				);

		List<TravelPlanDto> plans = planRows.stream()
				.map(planRow -> {
					return TravelPlanDto.builder()
							.planId(planRow.getPlanId())
							.title(planRow.getTitle())
							.createdAt(planRow.getCreatedAt().toString())
							.updatedAt(planRow.getUpdatedAt().toString())
							.beginDate(planRow.getBeginDate().toString())
							.endDate(planRow.getEndDate().toString())
							.memo(planRow.getMemo())
							.build();
				})
				.toList();

		return PlansResponseDto.builder()
				.travelPlans(plans)
				.build();
	}

	@Override
	public void addPlan(final TravelPlanRowDto travelPlanRow) {
		mapper.insertPlan(travelPlanRow);
	}
}

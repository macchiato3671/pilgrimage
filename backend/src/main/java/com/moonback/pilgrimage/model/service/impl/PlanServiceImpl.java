package com.moonback.pilgrimage.model.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.PlanErrorCode;
import com.moonback.pilgrimage.model.dto.TravelPlanDetailDto;
import com.moonback.pilgrimage.model.dto.TravelPlanDetailRowDto;
import com.moonback.pilgrimage.model.dto.TravelPlanDto;
import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;
import com.moonback.pilgrimage.model.dto.response.PlanResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;
import com.moonback.pilgrimage.model.dto.response.TravelPlanRowResponseDto;
import com.moonback.pilgrimage.model.mapper.PlanMapper;
import com.moonback.pilgrimage.model.service.PlanService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {
	private static final DateTimeFormatter PLAN_DETAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

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
	public PlanResponseDto getPlan(
			final int memberId,
			final int planId
			) {
		TravelPlanRowDto planRow = mapper.selectPlan(
				memberId,
				planId
				);

		if (planRow == null)
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_NOT_FOUND);

		List<TravelPlanDetailRowDto> detailRows = mapper.selectPlanDetails(planId);

		List<TravelPlanDetailDto> details = detailRows.stream()
				.map(detailRow -> {
					return TravelPlanDetailDto.builder()
							.dayNo(detailRow.getDayNo())
							.beginTime(detailRow.getBeginTime().format(PLAN_DETAIL_TIME_FORMATTER))
							.sceneId(detailRow.getSceneId())
							.placeId(detailRow.getPlaceId())
							.build();
				})
				.toList();

		return PlanResponseDto.builder()
				.planId(planRow.getPlanId())
				.title(planRow.getTitle())
				.createdAt(planRow.getCreatedAt().toString())
				.updatedAt(planRow.getUpdatedAt().toString())
				.beginDate(planRow.getBeginDate().toString())
				.endDate(planRow.getEndDate().toString())
				.memo(planRow.getMemo())
				.details(details)
				.build();
	}

	@Override
	public TravelPlanRowResponseDto updatePlan(
			final int memberId,
			final int planId,
			final TravelPlanRowDto travelPlanRow
			) {
		TravelPlanRowDto savedPlan = mapper.selectPlanByPlanId(planId);

		if (savedPlan == null)
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_NOT_FOUND);

		if (!savedPlan.getMemberId().equals(memberId))
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED);

		travelPlanRow.setPlanId(planId);
		travelPlanRow.setMemberId(memberId);

		mapper.updatePlan(travelPlanRow);

		TravelPlanRowDto updatedPlan = mapper.selectPlanByPlanId(planId);

		return TravelPlanRowResponseDto.builder()
				.planId(updatedPlan.getPlanId())
				.memberId(updatedPlan.getMemberId())
				.title(updatedPlan.getTitle())
				.createdAt(updatedPlan.getCreatedAt().toString())
				.updatedAt(updatedPlan.getUpdatedAt().toString())
				.beginDate(updatedPlan.getBeginDate().toString())
				.endDate(updatedPlan.getEndDate().toString())
				.memo(updatedPlan.getMemo())
				.build();
	}

	@Override
	public void deletePlan(
			final int memberId,
			final int planId
			) {
		TravelPlanRowDto savedPlan = mapper.selectPlanByPlanId(planId);

		if (savedPlan == null)
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_NOT_FOUND);

		if (!savedPlan.getMemberId().equals(memberId))
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED);

		mapper.deletePlan(
				memberId,
				planId
				);
	}

	@Override
	public void addPlan(final TravelPlanRowDto travelPlanRow) {
		mapper.insertPlan(travelPlanRow);
	}
}

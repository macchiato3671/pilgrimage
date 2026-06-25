package com.moonback.pilgrimage.model.service.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.PlanErrorCode;
import com.moonback.pilgrimage.model.dto.TravelPlanDetailDto;
import com.moonback.pilgrimage.model.dto.TravelPlanDetailRowDto;
import com.moonback.pilgrimage.model.dto.TravelPlanDto;
import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;
import com.moonback.pilgrimage.model.dto.request.TravelPlanDetailRequestDto;
import com.moonback.pilgrimage.model.dto.response.PlanDetailsResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlanResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;
import com.moonback.pilgrimage.model.dto.response.TravelPlanDetailResponseDto;
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
	@Transactional
	public void deletePlan(
			final int memberId,
			final int planId
			) {
		TravelPlanRowDto savedPlan = mapper.selectPlanByPlanId(planId);

		if (savedPlan == null)
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_NOT_FOUND);

		if (!savedPlan.getMemberId().equals(memberId))
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED);

		mapper.deletePlanDetails(planId);
		mapper.deletePlan(
				memberId,
				planId
				);
	}

	@Override
	@Transactional
	public PlanDetailsResponseDto updatePlanDetails(
			final int memberId,
			final int planId,
			final List<TravelPlanDetailRequestDto> planDetails
			) {
		TravelPlanRowDto savedPlan = mapper.selectPlanByPlanId(planId);

		if (savedPlan == null)
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_NOT_FOUND);

		if (!savedPlan.getMemberId().equals(memberId))
			throw new BusinessException(PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED);

		List<TravelPlanDetailRowDto> savedDetails = mapper.selectPlanDetails(planId);
		Set<Integer> savedDetailIds = savedDetails.stream()
				.map(TravelPlanDetailRowDto::getDetailId)
				.collect(Collectors.toSet());
		long travelDays = ChronoUnit.DAYS.between(
				savedPlan.getBeginDate(),
				savedPlan.getEndDate()
				) + 1;

		for (TravelPlanDetailRequestDto planDetail : planDetails) {
			validatePlanDetailUpdateArg(
					planDetail,
					savedDetailIds,
					travelDays
					);
		}

		List<Integer> requestDetailIds = planDetails.stream()
				.map(TravelPlanDetailRequestDto::getDetailId)
				.filter(detailId -> detailId != null)
				.toList();

		if (requestDetailIds.isEmpty())
			mapper.deletePlanDetails(planId);
		else
			mapper.deletePlanDetailsExcept(
					planId,
					requestDetailIds
					);

		for (TravelPlanDetailRequestDto planDetail : planDetails) {
			TravelPlanDetailRowDto planDetailRow = TravelPlanDetailRowDto.builder()
					.detailId(planDetail.getDetailId())
					.planId(planId)
					.placeId(planDetail.getPlaceId())
					.sceneId(planDetail.getSceneId())
					.dayNo(planDetail.getDayNo())
					.beginTime(LocalTime.parse(
							planDetail.getBeginTime(),
							PLAN_DETAIL_TIME_FORMATTER
							))
					.build();

			if (planDetail.getDetailId() == null)
				mapper.insertPlanDetail(planDetailRow);
			else
				mapper.updatePlanDetail(planDetailRow);
		}

		return buildPlanDetailsResponse(mapper.selectPlanDetails(planId));
	}

	@Override
	public void addPlan(final TravelPlanRowDto travelPlanRow) {
		mapper.insertPlan(travelPlanRow);
	}

	private void validatePlanDetailUpdateArg(
			final TravelPlanDetailRequestDto arg,
			final Set<Integer> savedDetailIds,
			final long travelDays
			) {
		if (arg.getDetailId() != null && !savedDetailIds.contains(arg.getDetailId()))
			throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL);

		if (arg.getDayNo() > travelDays)
			throw new BusinessException(PlanErrorCode.PLAN_DETAIL_OUT_OF_RANGE);

		if (arg.getPlaceId() != null && mapper.countPlaceByPlaceId(arg.getPlaceId()) == 0)
			throw new BusinessException(PlanErrorCode.PLACE_NOT_FOUND);

		if (arg.getSceneId() != null && mapper.countSceneBySceneId(arg.getSceneId()) == 0)
			throw new BusinessException(PlanErrorCode.SCENE_NOT_FOUND);
	}

	private PlanDetailsResponseDto buildPlanDetailsResponse(final List<TravelPlanDetailRowDto> detailRows) {
		List<TravelPlanDetailResponseDto> details = detailRows.stream()
				.map(detailRow -> {
					return TravelPlanDetailResponseDto.builder()
							.detailId(detailRow.getDetailId())
							.dayNo(detailRow.getDayNo())
							.beginTime(detailRow.getBeginTime().format(PLAN_DETAIL_TIME_FORMATTER))
							.sceneId(detailRow.getSceneId())
							.placeId(detailRow.getPlaceId())
							.build();
				})
				.toList();

		return PlanDetailsResponseDto.builder()
				.details(details)
				.build();
	}
}

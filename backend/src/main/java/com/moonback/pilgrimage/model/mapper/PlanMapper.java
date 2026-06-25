package com.moonback.pilgrimage.model.mapper;

import java.util.List;

import com.moonback.pilgrimage.model.dto.TravelPlanDetailRowDto;
import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;

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
	TravelPlanRowDto selectPlanByPlanId(
			final int planId
			);
	List<TravelPlanDetailRowDto> selectPlanDetails(
			final int planId
			);
	int countPlaceByPlaceId(
			final int placeId
			);
	int countSceneBySceneId(
			final int sceneId
			);
	void insertPlan(
			final TravelPlanRowDto dto
			);
	void insertPlanDetail(
			final TravelPlanDetailRowDto dto
			);
	void updatePlan(
			final TravelPlanRowDto dto
			);
	void updatePlanDetail(
			final TravelPlanDetailRowDto dto
			);
	void deletePlan(
			final int memberId,
			final int planId
			);
	void deletePlanDetails(
			final int planId
			);
	void deletePlanDetailsExcept(
			final int planId,
			final List<Integer> detailIds
			);

}

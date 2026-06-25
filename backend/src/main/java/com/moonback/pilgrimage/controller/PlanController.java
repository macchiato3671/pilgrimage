package com.moonback.pilgrimage.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.PlanErrorCode;
import com.moonback.pilgrimage.model.dto.TravelPlanRowDto;
import com.moonback.pilgrimage.model.dto.request.PlanDetailsRequestDto;
import com.moonback.pilgrimage.model.dto.request.TravelPlanDetailRequestDto;
import com.moonback.pilgrimage.model.dto.response.PlanDetailsResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlanResponseDto;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;
import com.moonback.pilgrimage.model.dto.response.TravelPlanRowResponseDto;
import com.moonback.pilgrimage.model.service.PlanService;
import com.moonback.pilgrimage.validator.MemberValidator;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {
	private static final int MAX_PAGE_SIZE = 50;
	private static final String DEFAULT_PAGE = "1";
	private static final String DEFAULT_PAGE_SIZE = "10";
	private static final String PLAN_DETAIL_TIME_REGEX = "([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d";

	private final PlanService service;
	private final MemberValidator validator;

	@GetMapping("")
	public ResponseEntity<PlansResponseDto> getPlans(
			@RequestParam(defaultValue = DEFAULT_PAGE) Integer page,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		validatePageArg(page);
		validatePageSizeArg(pageSize);

		PlansResponseDto responseDto = service.getPlans(
				memberId,
				page,
				pageSize
				);

		return ResponseEntity.status(HttpStatus.OK).body(responseDto);
	}

	@GetMapping("/{planId}")
	public ResponseEntity<PlanResponseDto> getPlan(
			@PathVariable Integer planId
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		PlanResponseDto responseDto = service.getPlan(
				memberId,
				planId
				);

		return ResponseEntity.status(HttpStatus.OK).body(responseDto);
	}

	@PostMapping("")
	public ResponseEntity<PlanResponseDto> postPlan(
			@RequestBody TravelPlanRowDto travelPlanRowDto
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		validateTravelPlanRowDtoArg(travelPlanRowDto);
		travelPlanRowDto.setMemberId(memberId);

		PlanResponseDto responseDto = service.addPlan(travelPlanRowDto);

		return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
	}

	@PutMapping("/{planId}")
	public ResponseEntity<TravelPlanRowResponseDto> putPlan(
			@PathVariable String planId,
			@RequestBody TravelPlanRowDto travelPlanRowDto
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		int planIdArg = parsePlanIdArg(planId);
		validateTravelPlanRowDtoArg(travelPlanRowDto);

		TravelPlanRowResponseDto responseDto = service.updatePlan(
				memberId,
				planIdArg,
				travelPlanRowDto
				);

		return ResponseEntity.status(HttpStatus.OK).body(responseDto);
	}

	@DeleteMapping("/{planId}")
	public ResponseEntity<Void> deletePlan(
			@PathVariable String planId
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		int planIdArg = parsePlanIdArg(planId);

		service.deletePlan(
				memberId,
				planIdArg
				);

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PutMapping("/{planId}/details")
	public ResponseEntity<PlanDetailsResponseDto> putPlanDetails(
			@PathVariable String planId,
			@RequestBody PlanDetailsRequestDto planDetailsRequestDto
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		int planIdArg = parsePlanIdArg(planId);
		validatePlanDetailsRequestDtoArg(planDetailsRequestDto);

		PlanDetailsResponseDto responseDto = service.updatePlanDetails(
				memberId,
				planIdArg,
				planDetailsRequestDto.getDetails()
				);

		return ResponseEntity.status(HttpStatus.OK).body(responseDto);
	}

	private void validatePageArg(final int arg) {
		if (arg < 1)
			throw new BusinessException(PlanErrorCode.INVALID_PAGE_ARG);
	}
	private void validatePageSizeArg(final int arg) {
		if (arg < 1 || arg > MAX_PAGE_SIZE)
			throw new BusinessException(PlanErrorCode.INVALID_PAGE_SIZE_ARG);
	}
	private void validateTravelPlanRowDtoArg(final TravelPlanRowDto arg) {
		if (arg == null || arg.getTitle() == null || arg.getBeginDate() == null || arg.getEndDate() == null)
			throw new BusinessException(PlanErrorCode.REQUIRED_FIELD_MISSING);

		if (arg.getTitle().isBlank())
			throw new BusinessException(PlanErrorCode.INVALID_TRAVEL_PLAN_TITLE);

		if (arg.getBeginDate().isAfter(arg.getEndDate()))
			throw new BusinessException(PlanErrorCode.INVALID_TRAVEL_PLAN_DATE);
	}
	private void validatePlanDetailsRequestDtoArg(final PlanDetailsRequestDto arg) {
		if (arg == null || arg.getDetails() == null || arg.getDetails().isEmpty())
			throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL);

		Set<Integer> detailIds = new HashSet<>();

		for (TravelPlanDetailRequestDto detail : arg.getDetails()) {
			if (detail == null || detail.getDayNo() == null || detail.getBeginTime() == null)
				throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL);

			if (detail.getDetailId() != null && (detail.getDetailId() < 1 || !detailIds.add(detail.getDetailId())))
				throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL);

			if (detail.getDayNo() < 1)
				throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL);

			if (detail.getBeginTime().isBlank() || !detail.getBeginTime().matches(PLAN_DETAIL_TIME_REGEX))
				throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL_TIME);

			if ((detail.getSceneId() == null && detail.getPlaceId() == null)
					|| (detail.getSceneId() != null && detail.getPlaceId() != null))
				throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL_TARGET);

			if ((detail.getSceneId() != null && detail.getSceneId() < 1)
					|| (detail.getPlaceId() != null && detail.getPlaceId() < 1))
				throw new BusinessException(PlanErrorCode.INVALID_PLAN_DETAIL_TARGET);
		}
	}
	private int parsePlanIdArg(final String arg) {
		if (arg == null || arg.isBlank())
			throw new BusinessException(PlanErrorCode.INVALID_PLAN_ID);

		try {
			int planId = Integer.parseInt(arg);
			validatePlanIdArg(planId);
			return planId;
		} catch (NumberFormatException e) {
			throw new BusinessException(PlanErrorCode.INVALID_PLAN_ID);
		}
	}
	private void validatePlanIdArg(final int arg) {
		if (arg < 1)
			throw new BusinessException(PlanErrorCode.INVALID_PLAN_ID);
	}
}

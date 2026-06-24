package com.moonback.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	public ResponseEntity<Void> postPlan(
			@RequestBody TravelPlanRowDto travelPlanRowDto
			) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		int memberId = Integer.parseInt((String)authentication.getPrincipal());
		validator.validateActiveMember(memberId);

		validateTravelPlanRowDtoArg(travelPlanRowDto);
		travelPlanRowDto.setMemberId(memberId);

		service.addPlan(travelPlanRowDto);

		return ResponseEntity.status(HttpStatus.CREATED).build();
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

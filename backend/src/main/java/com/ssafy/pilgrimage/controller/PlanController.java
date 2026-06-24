package com.ssafy.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.PlanErrorCode;
import com.ssafy.pilgrimage.model.dto.TravelPlanRowDto;
import com.ssafy.pilgrimage.model.dto.response.PlansResponseDto;
import com.ssafy.pilgrimage.model.service.PlanService;
import com.ssafy.pilgrimage.validator.MemberValidator;

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
}

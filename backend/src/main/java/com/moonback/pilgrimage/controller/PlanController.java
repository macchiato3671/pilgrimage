package com.moonback.pilgrimage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.PlanErrorCode;
import com.moonback.pilgrimage.model.dto.response.PlansResponseDto;
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

		PlansResponseDto dto = service.getPlans(
				memberId,
				page,
				pageSize
				);

		return ResponseEntity.status(HttpStatus.OK).body(dto);
	}

	private void validatePageArg(final int pageArg) {
		if (pageArg < 1)
			throw new BusinessException(PlanErrorCode.INVALID_PAGE_ARG);
	}
	private void validatePageSizeArg(final int pageSizeArg) {
		if (pageSizeArg < 1 || pageSizeArg > MAX_PAGE_SIZE)
			throw new BusinessException(PlanErrorCode.INVALID_PAGE_SIZE_ARG);
	}
}

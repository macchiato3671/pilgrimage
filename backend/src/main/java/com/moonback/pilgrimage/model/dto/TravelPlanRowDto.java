package com.moonback.pilgrimage.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanRowDto {
	private Integer planId;
	private Integer memberId;
	private String title;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDate beginDate;
	private LocalDate endDate;
}

package com.ssafy.pilgrimage.model.dto;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanDetailRowDto {
	private Integer detailId;
	private Integer planId;
	private Integer placeId;
	private Integer sceneId;
	private Integer dayNo;
	private LocalTime beginTime;
}

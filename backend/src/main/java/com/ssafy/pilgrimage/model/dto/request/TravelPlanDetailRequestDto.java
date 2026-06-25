package com.ssafy.pilgrimage.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlanDetailRequestDto {
	private Integer detailId;
	private Integer dayNo;
	private String beginTime;
	private Integer sceneId;
	private Integer placeId;
}

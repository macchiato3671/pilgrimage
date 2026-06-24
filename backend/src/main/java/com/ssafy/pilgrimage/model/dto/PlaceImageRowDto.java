package com.ssafy.pilgrimage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceImageRowDto {
	private int placeId;
	private int imgId;
	private String url;
}

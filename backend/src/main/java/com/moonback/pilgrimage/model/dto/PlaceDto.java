package com.moonback.pilgrimage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDto {
	
	private int placeId;
    private int contentId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String description;

    private int contentTypeId;
    private String contentTypeName;
}

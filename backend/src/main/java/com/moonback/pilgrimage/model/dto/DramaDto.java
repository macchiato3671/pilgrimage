package com.moonback.pilgrimage.model.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DramaDto {
	private int dramaId;
	private String title;
	private LocalDate releasedAt;
	private String description;
}

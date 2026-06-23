package com.ssafy.pilgrimage.batch.ingest.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SceneInsertParam {

	private Integer sceneId;
	private int dramaId;
	private String name;
	private String description;
	private String address;
	private double latitude;
	private double longitude;
	private byte[] ingestKey;
}

package com.ssafy.pilgrimage.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class PlaceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void place_상세_조회_성공() throws Exception {
		// given
		int placeId = 1;

		// when & then
		mockMvc.perform(get("/api/v1/places/{placeId}", placeId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.placeId").value(placeId))
				.andExpect(jsonPath("$.contentId").exists())
				.andExpect(jsonPath("$.name").exists())
				.andExpect(jsonPath("$.address").exists())
				.andExpect(jsonPath("$.latitude").exists())
				.andExpect(jsonPath("$.longitude").exists())
				.andExpect(jsonPath("$.description").exists())
				.andExpect(jsonPath("$.contentType").exists())
				.andExpect(jsonPath("$.contentType.contentTypeId").exists())
				.andExpect(jsonPath("$.contentType.name").exists())
				.andExpect(jsonPath("$.images").isArray());
	}

	@Test
	void place_상세_조회_실패() throws Exception {
		// given
		int placeId = 999999;

		// when & then
		mockMvc.perform(get("/api/v1/places/{placeId}", placeId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errorCode").value("PLACE_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Place does not exists."));
	}
}

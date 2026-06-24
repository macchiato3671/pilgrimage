package com.moonback.pilgrimage.integration;

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

import com.moonback.pilgrimage.model.dto.PlaceDto;
import com.moonback.pilgrimage.model.mapper.PlaceMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class PlaceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PlaceMapper placeMapper;

	@Test
	void place_detail_success() throws Exception {
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
				.andExpect(jsonPath("$.contentTypeId").exists())
				.andExpect(jsonPath("$.contentTypeName").exists())
				.andExpect(jsonPath("$.images").isArray());
	}

	@Test
	void place_detail_fail() throws Exception {
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

	@Test
	void place_search_success() throws Exception {
		// given
		int placeId = 1;
		PlaceDto place = placeMapper.getPlace(placeId);

		// when & then
		mockMvc.perform(get("/api/v1/places/search")
						.param("contentTypeId", String.valueOf(place.getContentTypeId()))
						.param("latitude", String.valueOf(place.getLatitude()))
						.param("longitude", String.valueOf(place.getLongitude()))
						.param("radiusKm", "3")
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.places").isArray())
				.andExpect(jsonPath("$.places[0].placeId").value(placeId))
				.andExpect(jsonPath("$.places[0].contentId").exists())
				.andExpect(jsonPath("$.places[0].contentTypeId").value(place.getContentTypeId()))
				.andExpect(jsonPath("$.places[0].contentTypeName").exists())
				.andExpect(jsonPath("$.places[0].name").exists())
				.andExpect(jsonPath("$.places[0].address").exists())
				.andExpect(jsonPath("$.places[0].latitude").exists())
				.andExpect(jsonPath("$.places[0].longitude").exists())
				.andExpect(jsonPath("$.places[0].description").exists())
				.andExpect(jsonPath("$.places[0].images").isArray())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").exists())
				.andExpect(jsonPath("$.page.totalPages").exists())
				.andExpect(jsonPath("$.page.hasNext").exists())
				.andExpect(jsonPath("$.page.hasPrevious").value(false));
	}

	@Test
	void place_search_required_condition_fail() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/places/search")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUIRED_SEARCH_CONDITION"))
				.andExpect(jsonPath("$.message").value("At least one search condition is required."));
	}

	@Test
	void place_search_invalid_page_fail() throws Exception {
		// given
		int placeId = 1;
		PlaceDto place = placeMapper.getPlace(placeId);

		// when & then
		mockMvc.perform(get("/api/v1/places/search")
						.param("contentTypeId", String.valueOf(place.getContentTypeId()))
						.param("page", "0")
						.param("size", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("INVALID_PAGE_REQUEST"))
				.andExpect(jsonPath("$.message").value("Invalid page request."));
	}
}

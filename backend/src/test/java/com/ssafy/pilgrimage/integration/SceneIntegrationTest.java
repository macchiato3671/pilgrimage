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
public class SceneIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void scene_detail_success() throws Exception {
		// given
		int sceneId = 1;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}", sceneId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sceneId").value(sceneId))
				.andExpect(jsonPath("$.dramaId").exists())
				.andExpect(jsonPath("$.name").exists())
				.andExpect(jsonPath("$.description").exists())
				.andExpect(jsonPath("$.address").exists())
				.andExpect(jsonPath("$.latitude").exists())
				.andExpect(jsonPath("$.longitude").exists())
				.andExpect(jsonPath("$.images").isArray());
	}

	@Test
	void scene_detail_fail() throws Exception {
		// given
		int sceneId = 999999;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}", sceneId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errorCode").value("SCENE_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Scene does not exists."));
	}

	@Test
	void scene_near_place_success() throws Exception {
		// given
		int sceneId = 1;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}/nearby-attractions", sceneId)
						.param("contentTypeId", "12")
						.param("radiusKm", "10000")
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sceneId").value(sceneId))
				.andExpect(jsonPath("$.sceneName").exists())
				.andExpect(jsonPath("$.sceneLatitude").exists())
				.andExpect(jsonPath("$.sceneLongitude").exists())
				.andExpect(jsonPath("$.radiusKm").value(10000.0))
				.andExpect(jsonPath("$.attractions").isArray())
				.andExpect(jsonPath("$.attractions[0].placeId").exists())
				.andExpect(jsonPath("$.attractions[0].contentId").exists())
				.andExpect(jsonPath("$.attractions[0].name").exists())
				.andExpect(jsonPath("$.attractions[0].description").exists())
				.andExpect(jsonPath("$.attractions[0].address").exists())
				.andExpect(jsonPath("$.attractions[0].latitude").exists())
				.andExpect(jsonPath("$.attractions[0].longitude").exists())
				.andExpect(jsonPath("$.attractions[0].images").isArray())
				.andExpect(jsonPath("$.attractions[0].imgUrl").doesNotExist())
				.andExpect(jsonPath("$.attractions[0].contentTypeId").value(12))
				.andExpect(jsonPath("$.attractions[0].contentTypeName").exists())
				.andExpect(jsonPath("$.attractions[0].distanceKm").exists())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").exists())
				.andExpect(jsonPath("$.totalPages").exists())
				.andExpect(jsonPath("$.hasNext").exists());
	}

	@Test
	void scene_near_place_scene_not_found() throws Exception {
		// given
		int sceneId = 999999;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}/nearby-attractions", sceneId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errorCode").value("SCENE_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Scene does not exists."));
	}

	@Test
	void scene_near_place_invalid_radius() throws Exception {
		// given
		int sceneId = 1;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}/nearby-attractions", sceneId)
						.param("radiusKm", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("INVALID_RADIUS_PARAMETER"))
				.andExpect(jsonPath("$.message").value("Invalid radius parameter."));
	}

	@Test
	void scene_near_place_invalid_page() throws Exception {
		// given
		int sceneId = 1;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}/nearby-attractions", sceneId)
						.param("size", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("INVALID_PAGE_PARAMETER"))
				.andExpect(jsonPath("$.message").value("Invalid page parameter."));
	}

	@Test
	void scene_near_place_invalid_content_type() throws Exception {
		// given
		int sceneId = 1;

		// when & then
		mockMvc.perform(get("/api/v1/scenes/{sceneId}/nearby-attractions", sceneId)
						.param("contentTypeId", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("INVALID_CONTENT_TYPE_ID"))
				.andExpect(jsonPath("$.message").value("Invalid content type ID."));
	}
}

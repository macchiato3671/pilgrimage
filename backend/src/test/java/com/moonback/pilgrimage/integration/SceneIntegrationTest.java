package com.moonback.pilgrimage.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.support.AbstractMySqlIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class SceneIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final AtomicInteger TEST_IDS = new AtomicInteger(200_000);
	private int sceneId;

	@BeforeEach
	void setUpSceneData() {
		int dramaId = TEST_IDS.getAndIncrement();
		jdbcTemplate.update(
				"INSERT INTO drama (drama_id, title, released_at, description) VALUES (?, ?, ?, ?)",
				dramaId,
				"장면 테스트 드라마",
				"2024-01-01",
				"장면 통합 테스트용 드라마");

		KeyHolder sceneKey = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO scene (drama_id, name, description, address, latitude, longitude) "
							+ "VALUES (?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, dramaId);
			statement.setString(2, "테스트 장면");
			statement.setString(3, "통합 테스트용 장면");
			statement.setString(4, "서울특별시 중구 테스트로 1");
			statement.setDouble(5, 37.5665);
			statement.setDouble(6, 126.9780);
			return statement;
		}, sceneKey);
		sceneId = sceneKey.getKey().intValue();

		jdbcTemplate.update(
				"INSERT INTO place (content_id, content_type_id, name, address, latitude, longitude, "
						+ "description, src_created_at, src_updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				String.valueOf(TEST_IDS.getAndIncrement()),
				12,
				"장면 주변 관광지",
				"서울특별시 중구 테스트로 1",
				37.5665,
				126.9780,
				"장면 통합 테스트용 관광지",
				"2024-01-01 00:00:00",
				"2024-01-01 00:00:00");
	}

	@Test
	void scene_detail_success() throws Exception {
		// given
		int sceneId = this.sceneId;

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
		int sceneId = this.sceneId;

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
		int sceneId = this.sceneId;

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
		int sceneId = this.sceneId;

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
		int sceneId = this.sceneId;

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

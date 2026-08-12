package com.moonback.pilgrimage.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.support.AbstractMySqlIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class DramaIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final AtomicInteger TEST_IDS = new AtomicInteger(100_000);
	private int dramaId;
	private int genreId;

	@BeforeEach
	void setUpDramaData() {
		dramaId = TEST_IDS.getAndIncrement();
		genreId = TEST_IDS.getAndIncrement();

		jdbcTemplate.update(
				"INSERT INTO drama (drama_id, title, released_at, description) VALUES (?, ?, ?, ?)",
				dramaId,
				"테스트 드라마",
				"2024-01-01",
				"통합 테스트용 드라마");
		jdbcTemplate.update(
				"INSERT INTO genre (genre_id, name) VALUES (?, ?)",
				genreId,
				"테스트 장르 " + genreId);
		jdbcTemplate.update(
				"INSERT INTO dramagenre (drama_id, genre_id) VALUES (?, ?)",
				dramaId,
				genreId);
		jdbcTemplate.update(
				"INSERT INTO scene (drama_id, name, description, address, latitude, longitude) "
						+ "VALUES (?, ?, ?, ?, ?, ?)",
				dramaId,
				"테스트 장면",
				"통합 테스트용 장면",
				"서울특별시 중구 테스트로 1",
				37.5665,
				126.9780);
	}

	@Test
	void drama_scene_list_success() throws Exception {
		// given
		int dramaId = this.dramaId;

		// when & then
		mockMvc.perform(get("/api/v1/dramas/{dramaId}", dramaId)
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dramaId").value(dramaId))
				.andExpect(jsonPath("$.title").exists())
				.andExpect(jsonPath("$.scenes").isArray())
				.andExpect(jsonPath("$.scenes[0].sceneId").exists())
				.andExpect(jsonPath("$.scenes[0].name").exists())
				.andExpect(jsonPath("$.scenes[0].description").exists())
				.andExpect(jsonPath("$.scenes[0].address").exists())
				.andExpect(jsonPath("$.scenes[0].latitude").exists())
				.andExpect(jsonPath("$.scenes[0].longitude").exists())
				.andExpect(jsonPath("$.scenes[0].images").isArray())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").exists())
				.andExpect(jsonPath("$.page.totalPages").exists())
				.andExpect(jsonPath("$.page.hasNext").exists())
				.andExpect(jsonPath("$.page.hasPrevious").value(false));
	}

	@Test
	void drama_scene_list_not_found() throws Exception {
		// given
		int dramaId = 999999;

		// when & then
		mockMvc.perform(get("/api/v1/dramas/{dramaId}", dramaId)
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errorCode").value("DRAMA_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Drama does not exists"));
	}

	@Test
	void drama_scene_list_invalid_page() throws Exception {
		// given
		int dramaId = this.dramaId;

		// when & then
		mockMvc.perform(get("/api/v1/dramas/{dramaId}", dramaId)
						.param("page", "0")
						.param("size", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("INVALID_PAGE_REQUEST"))
				.andExpect(jsonPath("$.message").value("Invalid page request."));
	}

	@Test
	void drama_years_success() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/dramas/years")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.years").isArray());
	}

	@Test
	void drama_year_list_success() throws Exception {
		// given
		int year = 2024;

		// when & then
		mockMvc.perform(get("/api/v1/dramas/years/{year}", year)
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.year").value(year))
				.andExpect(jsonPath("$.dramas").isArray())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").exists())
				.andExpect(jsonPath("$.page.totalPages").exists())
				.andExpect(jsonPath("$.page.hasNext").exists())
				.andExpect(jsonPath("$.page.hasPrevious").value(false));
	}

	@Test
	void drama_genres_success() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/dramas/genres")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.genres").isArray())
				.andExpect(jsonPath("$.genres[0].genreId").exists())
				.andExpect(jsonPath("$.genres[0].name").exists());
	}

	@Test
	void drama_genre_list_success() throws Exception {
		// given
		int genreId = this.genreId;

		// when & then
		mockMvc.perform(get("/api/v1/dramas/genres/{genreId}", genreId)
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.genreId").value(genreId))
				.andExpect(jsonPath("$.name").exists())
				.andExpect(jsonPath("$.dramas").isArray())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").exists())
				.andExpect(jsonPath("$.page.totalPages").exists())
				.andExpect(jsonPath("$.page.hasNext").exists())
				.andExpect(jsonPath("$.page.hasPrevious").value(false));
	}

	@Test
	void drama_search_success() throws Exception {
		// given
		String keyword = "";

		// when & then
		mockMvc.perform(get("/api/v1/dramas/search")
						.param("keyword", keyword)
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.keyword").value(keyword))
				.andExpect(jsonPath("$.dramas").isArray())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").exists())
				.andExpect(jsonPath("$.page.totalPages").exists())
				.andExpect(jsonPath("$.page.hasNext").exists())
				.andExpect(jsonPath("$.page.hasPrevious").value(false));
	}
}

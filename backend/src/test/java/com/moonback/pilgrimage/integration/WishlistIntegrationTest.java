package com.moonback.pilgrimage.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.moonback.pilgrimage.support.AbstractMySqlIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class WishlistIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final AtomicInteger TEST_IDS = new AtomicInteger(300_000);
	private int memberId;
	private int dramaId;
	private int sceneId;
	private int secondSceneId;

	@BeforeEach
	void setUpWishlistData() {
		memberId = insertMember();
		dramaId = TEST_IDS.getAndIncrement();
		jdbcTemplate.update(
				"INSERT INTO drama (drama_id, title, released_at, description) VALUES (?, ?, ?, ?)",
				dramaId,
				"위시리스트 테스트 드라마",
				"2024-01-01",
				"위시리스트 통합 테스트용 드라마");

		int genreId = TEST_IDS.getAndIncrement();
		jdbcTemplate.update(
				"INSERT INTO genre (genre_id, name) VALUES (?, ?)",
				genreId,
				"위시리스트 테스트 장르 " + genreId);
		jdbcTemplate.update(
				"INSERT INTO dramagenre (drama_id, genre_id) VALUES (?, ?)",
				dramaId,
				genreId);

		sceneId = insertScene("위시리스트 테스트 장면");
		secondSceneId = insertScene("위시리스트 보조 장면");
	}

	private int insertMember() {
		String email = "wishlist-" + System.nanoTime() + "@example.com";
		jdbcTemplate.update(
				"INSERT INTO member (email, password, nickname, role_id, status_id) VALUES (?, ?, ?, ?, ?)",
				email,
				"test-password",
				"위시리스트 테스트 사용자",
				1,
				1);
		return jdbcTemplate.queryForObject(
				"SELECT member_id FROM member WHERE email = ?",
				Integer.class,
				email);
	}

	private int insertScene(String name) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO scene (drama_id, name, description, address, latitude, longitude) "
							+ "VALUES (?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, dramaId);
			statement.setString(2, name);
			statement.setString(3, "위시리스트 통합 테스트용 장면 설명");
			statement.setString(4, "서울특별시 중구 테스트로 1");
			statement.setDouble(5, 37.5665);
			statement.setDouble(6, 126.9780);
			return statement;
		}, keyHolder);
		return keyHolder.getKey().intValue();
	}

	@Test
	void 위시리스트_추가_성공() throws Exception {
		// given
		int sceneId = this.sceneId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON));

		// when & then
		mockMvc.perform(post("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
	}

	@Test
	void 위시리스트_중복_추가() throws Exception {
		// given
		int sceneId = this.sceneId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON));

		mockMvc.perform(post("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// when & then
		mockMvc.perform(post("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.errorCode").value("WISHLIST_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.message").value("Scene already exists."));
	}

	@Test
	void 위시리스트_드라마_목록_조회() throws Exception {
		// given
		int sceneId = this.sceneId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON));

		mockMvc.perform(post("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// when & then
		mockMvc.perform(get("/api/v1/wishlist/dramas")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dramas").isArray())
				.andExpect(jsonPath("$.dramas[0].dramaId").exists())
				.andExpect(jsonPath("$.dramas[0].title").exists())
				.andExpect(jsonPath("$.dramas[0].releasedAt").exists())
				.andExpect(jsonPath("$.dramas[0].description").exists())
				.andExpect(jsonPath("$.dramas[0].images").isArray())
				.andExpect(jsonPath("$.dramas[0].genres").isArray());
	}

	@Test
	void 위시리스트_드라마별_씬_조회() throws Exception {
		// given
		int sceneId = this.secondSceneId;
		int dramaId = this.dramaId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON));

		mockMvc.perform(post("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// when & then
		mockMvc.perform(get("/api/v1/wishlist/dramas/{dramaId}/scenes", dramaId)
						.with(authentication(auth))
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scenes").isArray())
				.andExpect(jsonPath("$.scenes[0].wishlistId").exists())
				.andExpect(jsonPath("$.scenes[0].sceneId").value(sceneId))
				.andExpect(jsonPath("$.scenes[0].name").exists())
				.andExpect(jsonPath("$.scenes[0].address").exists())
				.andExpect(jsonPath("$.scenes[0].latitude").exists())
				.andExpect(jsonPath("$.scenes[0].longitude").exists())
				.andExpect(jsonPath("$.scenes[0].createdAt").exists())
				.andExpect(jsonPath("$.scenes[0].images").isArray())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").exists())
				.andExpect(jsonPath("$.page.totalPages").exists())
				.andExpect(jsonPath("$.page.hasNext").exists())
				.andExpect(jsonPath("$.page.hasPrevious").value(false));
	}

	@Test
	void 위시리스트_드라마별_씬_조회_페이지_오류() throws Exception {
		// given
		int dramaId = this.dramaId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		// when & then
		mockMvc.perform(get("/api/v1/wishlist/dramas/{dramaId}/scenes", dramaId)
						.with(authentication(auth))
						.param("page", "0")
						.param("size", "0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("INVALID_PAGE_REQUEST"))
				.andExpect(jsonPath("$.message").value("Invalid page request."));
	}

	@Test
	void 위시리스트_삭제_성공() throws Exception {
		// given
		int sceneId = this.sceneId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON));

		mockMvc.perform(post("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// when & then
		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void 위시리스트_삭제_실패() throws Exception {
		// given
		int sceneId = this.sceneId;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						memberId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON));

		// when & then
		mockMvc.perform(delete("/api/v1/wishlist/{sceneId}", sceneId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errorCode").value("WISHLIST_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Wishlist Scene does not exists"));
	}
}

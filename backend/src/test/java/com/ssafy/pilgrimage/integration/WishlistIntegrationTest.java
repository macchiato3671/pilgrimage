package com.ssafy.pilgrimage.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class WishlistIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 위시리스트_추가_성공() throws Exception {
		// given
		int sceneId = 1;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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
		int sceneId = 1;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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
		int sceneId = 1;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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
		int sceneId = 895;
		int dramaId = 2265;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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
		int dramaId = 1;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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
		int sceneId = 1;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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
		int sceneId = 1;

		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(
						1,
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

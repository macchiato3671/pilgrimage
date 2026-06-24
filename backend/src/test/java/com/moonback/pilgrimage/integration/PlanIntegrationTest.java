package com.moonback.pilgrimage.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.mapper.MemberMapper;
import com.moonback.pilgrimage.model.type.MemberRole;
import com.moonback.pilgrimage.model.type.MemberStatus;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
class PlanIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void API_009_여행_일정_만들기_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		Map<String, Object> request = Map.of(
				"title", "부산 국밥 투어",
				"beginDate", "2026-06-12",
				"endDate", "2026-06-13",
				"memo", "부산 여행 메모"
				);

		mockMvc.perform(post("/api/v1/plans")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM TravelPlan "
						+ "WHERE member_id = ? "
						+ "AND title = ? "
						+ "AND begin_date = ? "
						+ "AND end_date = ? "
						+ "AND memo = ?",
				Integer.class,
				member.getMemberId(),
				"부산 국밥 투어",
				"2026-06-12",
				"2026-06-13",
				"부산 여행 메모"
				);

		org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
	}

	@Test
	void API_010_여행_일정_리스트_조회_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		mockMvc.perform(get("/api/v1/plans")
						.with(authentication(auth))
						.param("page", "1")
						.param("pageSize", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.travelPlans").isArray())
				.andExpect(jsonPath("$.travelPlans[0].planId").exists())
				.andExpect(jsonPath("$.travelPlans[0].title").value("부산 국밥 투어"))
				.andExpect(jsonPath("$.travelPlans[0].createdAt").exists())
				.andExpect(jsonPath("$.travelPlans[0].updatedAt").exists())
				.andExpect(jsonPath("$.travelPlans[0].beginDate").value("2026-06-12"))
				.andExpect(jsonPath("$.travelPlans[0].endDate").value("2026-06-13"))
				.andExpect(jsonPath("$.travelPlans[0].memo").value("부산 여행 메모"));
	}

	@Test
	//@Disabled("API-011 상세 조회 엔드포인트 구현 후 활성화")
	void API_011_여행_일정_상세_조회_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		mockMvc.perform(get("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planId").value(planId))
				.andExpect(jsonPath("$.title").value("부산 국밥 투어"))
				.andExpect(jsonPath("$.beginDate").value("2026-06-12"))
				.andExpect(jsonPath("$.endDate").value("2026-06-13"))
				.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	@Disabled("API-014 수정 엔드포인트 구현 후 활성화")
	void API_014_여행_계획_수정_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		Map<String, Object> request = Map.of(
				"title", "부산 바다 투어",
				"beginDate", "2026-07-01",
				"endDate", "2026-07-03",
				"memo", "수정된 부산 여행 메모"
				);

		mockMvc.perform(put("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
	}

	@Test
	@Disabled("API-015 삭제 엔드포인트 구현 후 활성화")
	void API_015_여행_계획_삭제_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		mockMvc.perform(delete("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	private MemberDto activeMember() {
		MemberDto member = new MemberDto();
		member.setEmail("plan-integration-" + System.nanoTime() + "@example.com");
		member.setPassword("encoded-password");
		member.setNickname("일정테스트");
		member.setRoleId(MemberRole.USER.getId());
		member.setStatusId(MemberStatus.ACTIVE.getId());
		return member;
	}

	private UsernamePasswordAuthenticationToken auth(int memberId) {
		return new UsernamePasswordAuthenticationToken(
				String.valueOf(memberId),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);
	}

	private UsernamePasswordAuthenticationToken authenticate(int memberId) {
		UsernamePasswordAuthenticationToken auth = auth(memberId);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
		return auth;
	}

	private int insertPlan(
			int memberId,
			String title,
			String beginDate,
			String endDate,
			String memo
			) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO TravelPlan("
							+ "member_id, "
							+ "title, "
							+ "begin_date, "
							+ "end_date, "
							+ "memo"
							+ ") VALUES (?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS
					);
			ps.setInt(1, memberId);
			ps.setString(2, title);
			ps.setString(3, beginDate);
			ps.setString(4, endDate);
			ps.setString(5, memo);
			return ps;
		}, keyHolder);

		return keyHolder.getKey().intValue();
	}
}

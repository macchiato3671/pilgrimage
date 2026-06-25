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
import java.sql.Types;
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
	//@Disabled("API-014 수정 엔드포인트 구현 후 활성화")
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
	//@Disabled("API-015 삭제 엔드포인트 구현 후 활성화")
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

	@Test
	void API_033_여행_세부_일정_목록_수정_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int contentTypeId = insertContentType();
		int placeId = insertPlace(contentTypeId);
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);
		int updateDetailId = insertPlanDetail(planId, null, sceneId, 1, "10:30:00");
		int deleteDetailId = insertPlanDetail(planId, placeId, null, 2, "14:00:00");

		Map<String, Object> request = Map.of(
				"details", List.of(
						Map.of(
								"detailId", updateDetailId,
								"dayNo", 2,
								"beginTime", "11:00:00",
								"sceneId", sceneId
								),
						Map.of(
								"dayNo", 1,
								"beginTime", "09:00:00",
								"placeId", placeId
								)
						)
				);

		mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(jsonPath("$.details.length()").value(2))
				.andExpect(jsonPath("$.details[0].detailId").exists())
				.andExpect(jsonPath("$.details[0].dayNo").value(1))
				.andExpect(jsonPath("$.details[0].beginTime").value("09:00:00"))
				.andExpect(jsonPath("$.details[0].placeId").value(placeId))
				.andExpect(jsonPath("$.details[1].detailId").value(updateDetailId))
				.andExpect(jsonPath("$.details[1].dayNo").value(2))
				.andExpect(jsonPath("$.details[1].beginTime").value("11:00:00"))
				.andExpect(jsonPath("$.details[1].sceneId").value(sceneId));

		Integer totalCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM PlanDetail "
						+ "WHERE plan_id = ?",
				Integer.class,
				planId
				);
		Integer updatedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM PlanDetail "
						+ "WHERE detail_id = ? "
						+ "AND plan_id = ? "
						+ "AND scene_id = ? "
						+ "AND place_id IS NULL "
						+ "AND day_no = ? "
						+ "AND begin_time = ?",
				Integer.class,
				updateDetailId,
				planId,
				sceneId,
				2,
				"11:00:00"
				);
		Integer insertedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM PlanDetail "
						+ "WHERE plan_id = ? "
						+ "AND place_id = ? "
						+ "AND scene_id IS NULL "
						+ "AND day_no = ? "
						+ "AND begin_time = ?",
				Integer.class,
				planId,
				placeId,
				1,
				"09:00:00"
				);
		Integer deletedCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM PlanDetail "
						+ "WHERE detail_id = ?",
				Integer.class,
				deleteDetailId
				);

		org.assertj.core.api.Assertions.assertThat(totalCount).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(updatedCount).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(insertedCount).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(deletedCount).isZero();
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

	private int insertContentType() {
		int contentTypeId = nextPositiveId();
		jdbcTemplate.update(
				"INSERT INTO ContentType("
						+ "content_type_id, "
						+ "name"
						+ ") VALUES (?, ?)",
				contentTypeId,
				"테스트 타입 " + contentTypeId
				);
		return contentTypeId;
	}

	private int insertPlace(int contentTypeId) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO Place("
							+ "content_id, "
							+ "content_type_id, "
							+ "name, "
							+ "address, "
							+ "latitude, "
							+ "longitude, "
							+ "description, "
							+ "src_created_at, "
							+ "src_updated_at"
							+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS
					);
			ps.setString(1, "plan-integration-place-" + System.nanoTime());
			ps.setInt(2, contentTypeId);
			ps.setString(3, "테스트 장소");
			ps.setString(4, "부산광역시 테스트구");
			ps.setDouble(5, 35.1796);
			ps.setDouble(6, 129.0756);
			ps.setString(7, "테스트 장소 설명");
			ps.setString(8, "2026-06-01 00:00:00");
			ps.setString(9, "2026-06-01 00:00:00");
			return ps;
		}, keyHolder);

		return keyHolder.getKey().intValue();
	}

	private int insertDrama() {
		int dramaId = nextPositiveId();
		jdbcTemplate.update(
				"INSERT INTO Drama("
						+ "drama_id, "
						+ "title, "
						+ "released_at, "
						+ "description"
						+ ") VALUES (?, ?, ?, ?)",
				dramaId,
				"테스트 드라마",
				"2026-06-01",
				"테스트 드라마 설명"
				);
		return dramaId;
	}

	private int insertScene(int dramaId) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO Scene("
							+ "drama_id, "
							+ "name, "
							+ "description, "
							+ "address, "
							+ "latitude, "
							+ "longitude"
							+ ") VALUES (?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS
					);
			ps.setInt(1, dramaId);
			ps.setString(2, "테스트 씬");
			ps.setString(3, "테스트 씬 설명");
			ps.setString(4, "부산광역시 테스트동");
			ps.setDouble(5, 35.1796);
			ps.setDouble(6, 129.0756);
			return ps;
		}, keyHolder);

		return keyHolder.getKey().intValue();
	}

	private int insertPlanDetail(
			int planId,
			Integer placeId,
			Integer sceneId,
			int dayNo,
			String beginTime
			) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO PlanDetail("
							+ "plan_id, "
							+ "place_id, "
							+ "scene_id, "
							+ "day_no, "
							+ "begin_time"
							+ ") VALUES (?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS
					);
			ps.setInt(1, planId);
			if (placeId == null)
				ps.setNull(2, Types.INTEGER);
			else
				ps.setInt(2, placeId);
			if (sceneId == null)
				ps.setNull(3, Types.INTEGER);
			else
				ps.setInt(3, sceneId);
			ps.setInt(4, dayNo);
			ps.setString(5, beginTime);
			return ps;
		}, keyHolder);

		return keyHolder.getKey().intValue();
	}

	private int nextPositiveId() {
		return (int)Math.floorMod(System.nanoTime(), 1_000_000_000L) + 1;
	}
}

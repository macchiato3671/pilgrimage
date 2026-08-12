package com.moonback.pilgrimage.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonback.pilgrimage.exception.code.PlanErrorCode;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.mapper.MemberMapper;
import com.moonback.pilgrimage.model.type.MemberRole;
import com.moonback.pilgrimage.model.type.MemberStatus;
import com.moonback.pilgrimage.support.AbstractMySqlIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
class PlanIntegrationTest extends AbstractMySqlIntegrationTest {

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
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.planId").isNumber())
				.andExpect(jsonPath("$.title").value(request.get("title")))
				.andExpect(jsonPath("$.beginDate").value(request.get("beginDate")))
				.andExpect(jsonPath("$.endDate").value(request.get("endDate")))
				.andExpect(jsonPath("$.memo").value(request.get("memo")))
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(jsonPath("$.details.length()").value(0));

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

		assertThat(count).isEqualTo(1);
	}

	@Test
	void API_009_create_plan_missing_required_field_returns_REQUIRED_FIELD_MISSING() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		Map<String, Object> request = Map.of(
				"title", "Busan tour",
				"beginDate", "2026-06-12"
				);

		assertPlanError(
				mockMvc.perform(post("/api/v1/plans")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))),
				PlanErrorCode.REQUIRED_FIELD_MISSING
				);
		assertThat(countPlans(member.getMemberId())).isZero();
	}

	@Test
	void API_009_create_plan_blank_title_returns_INVALID_TRAVEL_PLAN_TITLE() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		Map<String, Object> request = Map.of(
				"title", " ",
				"beginDate", "2026-06-12",
				"endDate", "2026-06-13"
				);

		assertPlanError(
				mockMvc.perform(post("/api/v1/plans")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))),
				PlanErrorCode.INVALID_TRAVEL_PLAN_TITLE
				);
		assertThat(countPlans(member.getMemberId())).isZero();
	}

	@Test
	void API_009_create_plan_reversed_date_returns_INVALID_TRAVEL_PLAN_DATE() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		Map<String, Object> request = Map.of(
				"title", "Busan tour",
				"beginDate", "2026-06-13",
				"endDate", "2026-06-12"
				);

		assertPlanError(
				mockMvc.perform(post("/api/v1/plans")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))),
				PlanErrorCode.INVALID_TRAVEL_PLAN_DATE
				);
		assertThat(countPlans(member.getMemberId())).isZero();
	}

	@Test
	void API_010_여행_일정_리스트_조회_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		MemberDto otherMember = activeMember();
		memberMapper.insertMember(otherMember);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		insertPlan(otherMember.getMemberId(), "남의 여행", "2026-06-12", "2026-06-13", "남의 여행 메모");

		mockMvc.perform(get("/api/v1/plans")
						.with(authentication(auth))
						.param("page", "1")
						.param("pageSize", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.travelPlans").isArray())
				.andExpect(jsonPath("$.travelPlans.length()").value(1))
				.andExpect(jsonPath("$.travelPlans[0].planId").exists())
				.andExpect(jsonPath("$.travelPlans[0].title").value("부산 국밥 투어"))
				.andExpect(jsonPath("$.travelPlans[0].createdAt").exists())
				.andExpect(jsonPath("$.travelPlans[0].updatedAt").exists())
				.andExpect(jsonPath("$.travelPlans[0].beginDate").value("2026-06-12"))
				.andExpect(jsonPath("$.travelPlans[0].endDate").value("2026-06-13"))
				.andExpect(jsonPath("$.travelPlans[0].memo").value("부산 여행 메모"));
	}

	@Test
	void API_010_get_plans_invalid_page_returns_INVALID_PAGE_ARG() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(get("/api/v1/plans")
						.with(authentication(auth))
						.param("page", "0")
						.param("pageSize", "10")
						.contentType(MediaType.APPLICATION_JSON)),
				PlanErrorCode.INVALID_PAGE_ARG
				);
	}

	@Test
	void API_010_get_plans_invalid_page_size_returns_INVALID_PAGE_SIZE_ARG() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(get("/api/v1/plans")
						.with(authentication(auth))
						.param("page", "1")
						.param("pageSize", "51")
						.contentType(MediaType.APPLICATION_JSON)),
				PlanErrorCode.INVALID_PAGE_SIZE_ARG
				);
	}

	@Test
	//@Disabled("API-011 상세 조회 엔드포인트 구현 후 활성화")
	void API_011_여행_일정_상세_조회_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);
		insertPlanDetail(planId, null, sceneId, 1, "10:30:00");

		mockMvc.perform(get("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planId").value(planId))
				.andExpect(jsonPath("$.title").value("부산 국밥 투어"))
				.andExpect(jsonPath("$.beginDate").value("2026-06-12"))
				.andExpect(jsonPath("$.endDate").value("2026-06-13"))
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(jsonPath("$.details.length()").value(1))
				.andExpect(jsonPath("$.details[0].dayNo").value(1))
				.andExpect(jsonPath("$.details[0].beginTime").value("10:30:00"))
				.andExpect(jsonPath("$.details[0].sceneId").value(sceneId));
	}

	@Test
	void API_011_get_plan_not_found_returns_TRAVEL_PLAN_NOT_FOUND() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(get("/api/v1/plans/{planId}", nextPositiveId())
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)),
				PlanErrorCode.TRAVEL_PLAN_NOT_FOUND
				);
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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planId").value(planId))
				.andExpect(jsonPath("$.memberId").value(member.getMemberId()))
				.andExpect(jsonPath("$.title").value("부산 바다 투어"))
				.andExpect(jsonPath("$.beginDate").value("2026-07-01"))
				.andExpect(jsonPath("$.endDate").value("2026-07-03"))
				.andExpect(jsonPath("$.memo").value("수정된 부산 여행 메모"));

		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM TravelPlan "
						+ "WHERE plan_id = ? "
						+ "AND member_id = ? "
						+ "AND title = ? "
						+ "AND begin_date = ? "
						+ "AND end_date = ? "
						+ "AND memo = ?",
				Integer.class,
				planId,
				member.getMemberId(),
				"부산 바다 투어",
				"2026-07-01",
				"2026-07-03",
				"수정된 부산 여행 메모"
				);

		assertThat(count).isEqualTo(1);
	}

	@Test
	void API_014_update_plan_invalid_plan_id_returns_INVALID_PLAN_ID() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}", "abc")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validPlanRequest()))),
				PlanErrorCode.INVALID_PLAN_ID
				);
	}

	@Test
	void API_014_update_plan_missing_required_field_returns_REQUIRED_FIELD_MISSING() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		Map<String, Object> request = Map.of(
				"title", "Busan tour",
				"beginDate", "2026-06-12"
				);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))),
				PlanErrorCode.REQUIRED_FIELD_MISSING
				);
	}

	@Test
	void API_014_update_plan_reversed_date_returns_INVALID_TRAVEL_PLAN_DATE() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		Map<String, Object> request = Map.of(
				"title", "Busan tour",
				"beginDate", "2026-06-13",
				"endDate", "2026-06-12"
				);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))),
				PlanErrorCode.INVALID_TRAVEL_PLAN_DATE
				);
	}

	@Test
	void API_014_update_plan_not_found_returns_TRAVEL_PLAN_NOT_FOUND() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}", nextPositiveId())
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validPlanRequest()))),
				PlanErrorCode.TRAVEL_PLAN_NOT_FOUND
				);
	}

	@Test
	void API_014_update_plan_access_denied_returns_TRAVEL_PLAN_ACCESS_DENIED() throws Exception {
		MemberDto owner = activeMember();
		memberMapper.insertMember(owner);
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(owner.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validPlanRequest()))),
				PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED
				);

		assertThat(countPlans(owner.getMemberId())).isEqualTo(1);
	}

	@Test
	//@Disabled("API-015 삭제 엔드포인트 구현 후 활성화")
	void API_015_여행_계획_삭제_성공() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int contentTypeId = insertContentType();
		int placeId = insertPlace(contentTypeId);
		insertPlanDetail(planId, placeId, null, 1, "09:00:00");

		mockMvc.perform(delete("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		assertThat(countPlanByPlanId(planId)).isZero();
		assertThat(countPlanDetails(planId)).isZero();
	}

	@Test
	void API_015_delete_plan_invalid_plan_id_returns_INVALID_PLAN_ID() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(delete("/api/v1/plans/{planId}", "abc")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)),
				PlanErrorCode.INVALID_PLAN_ID
				);
	}

	@Test
	void API_015_delete_plan_not_found_returns_TRAVEL_PLAN_NOT_FOUND() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(delete("/api/v1/plans/{planId}", nextPositiveId())
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)),
				PlanErrorCode.TRAVEL_PLAN_NOT_FOUND
				);
	}

	@Test
	void API_015_delete_plan_access_denied_returns_TRAVEL_PLAN_ACCESS_DENIED() throws Exception {
		MemberDto owner = activeMember();
		memberMapper.insertMember(owner);
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(owner.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		assertPlanError(
				mockMvc.perform(delete("/api/v1/plans/{planId}", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)),
				PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED
				);

		assertThat(countPlanByPlanId(planId)).isEqualTo(1);
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

		assertThat(totalCount).isEqualTo(2);
		assertThat(updatedCount).isEqualTo(1);
		assertThat(insertedCount).isEqualTo(1);
		assertThat(deletedCount).isZero();
	}

	@Test
	void API_033_update_plan_details_invalid_plan_id_returns_INVALID_PLAN_ID() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", "abc")
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"details", List.of()
								)))),
				PlanErrorCode.INVALID_PLAN_ID
				);
	}

	@Test
	void API_033_update_plan_details_empty_details_returns_INVALID_PLAN_DETAIL() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"details", List.of()
								)))),
				PlanErrorCode.INVALID_PLAN_DETAIL
				);
	}

	@Test
	void API_033_update_plan_details_missing_required_field_returns_INVALID_PLAN_DETAIL() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								Map.of(
										"beginTime", "10:30:00",
										"sceneId", sceneId
										)
								))))),
				PlanErrorCode.INVALID_PLAN_DETAIL
				);
	}

	@Test
	void API_033_update_plan_details_duplicate_detail_id_returns_INVALID_PLAN_DETAIL() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);
		int detailId = insertPlanDetail(planId, null, sceneId, 1, "10:30:00");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(detailId, 1, "10:30:00", sceneId),
								sceneDetail(detailId, 2, "11:00:00", sceneId)
								))))),
				PlanErrorCode.INVALID_PLAN_DETAIL
				);
		assertThat(countPlanDetails(planId)).isEqualTo(1);
	}

	@Test
	void API_033_update_plan_details_invalid_time_returns_INVALID_PLAN_DETAIL_TIME() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(null, 1, "24:00:00", sceneId)
								))))),
				PlanErrorCode.INVALID_PLAN_DETAIL_TIME
				);
	}

	@Test
	void API_033_update_plan_details_both_targets_returns_INVALID_PLAN_DETAIL_TARGET() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int contentTypeId = insertContentType();
		int placeId = insertPlace(contentTypeId);
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								bothTargetDetail(1, "10:30:00", placeId, sceneId)
								))))),
				PlanErrorCode.INVALID_PLAN_DETAIL_TARGET
				);
	}

	@Test
	void API_033_update_plan_details_no_target_returns_INVALID_PLAN_DETAIL_TARGET() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								noTargetDetail(1, "10:30:00")
								))))),
				PlanErrorCode.INVALID_PLAN_DETAIL_TARGET
				);
	}

	@Test
	void API_033_update_plan_details_plan_not_found_returns_TRAVEL_PLAN_NOT_FOUND() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", nextPositiveId())
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(null, 1, "10:30:00", sceneId)
								))))),
				PlanErrorCode.TRAVEL_PLAN_NOT_FOUND
				);
	}

	@Test
	void API_033_update_plan_details_access_denied_returns_TRAVEL_PLAN_ACCESS_DENIED() throws Exception {
		MemberDto owner = activeMember();
		memberMapper.insertMember(owner);
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(owner.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(null, 1, "10:30:00", sceneId)
								))))),
				PlanErrorCode.TRAVEL_PLAN_ACCESS_DENIED
				);
	}

	@Test
	void API_033_update_plan_details_unknown_detail_id_returns_INVALID_PLAN_DETAIL() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int otherPlanId = insertPlan(member.getMemberId(), "다른 여행", "2026-06-12", "2026-06-13", "다른 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);
		int otherDetailId = insertPlanDetail(otherPlanId, null, sceneId, 1, "10:30:00");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(otherDetailId, 1, "10:30:00", sceneId)
								))))),
				PlanErrorCode.INVALID_PLAN_DETAIL
				);
		assertThat(countPlanDetails(otherPlanId)).isEqualTo(1);
	}

	@Test
	void API_033_update_plan_details_out_of_range_returns_PLAN_DETAIL_OUT_OF_RANGE() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");
		int dramaId = insertDrama();
		int sceneId = insertScene(dramaId);

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(null, 3, "10:30:00", sceneId)
								))))),
				PlanErrorCode.PLAN_DETAIL_OUT_OF_RANGE
				);
		assertThat(countPlanDetails(planId)).isZero();
	}

	@Test
	void API_033_update_plan_details_missing_place_returns_PLACE_NOT_FOUND() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								placeDetail(null, 1, "10:30:00", nextPositiveId())
								))))),
				PlanErrorCode.PLACE_NOT_FOUND
				);
	}

	@Test
	void API_033_update_plan_details_missing_scene_returns_SCENE_NOT_FOUND() throws Exception {
		MemberDto member = activeMember();
		memberMapper.insertMember(member);
		UsernamePasswordAuthenticationToken auth = authenticate(member.getMemberId());
		int planId = insertPlan(member.getMemberId(), "부산 국밥 투어", "2026-06-12", "2026-06-13", "부산 여행 메모");

		assertPlanError(
				mockMvc.perform(put("/api/v1/plans/{planId}/details", planId)
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(planDetailsRequest(List.of(
								sceneDetail(null, 1, "10:30:00", nextPositiveId())
								))))),
				PlanErrorCode.SCENE_NOT_FOUND
				);
	}

	private void assertPlanError(
			ResultActions resultActions,
			PlanErrorCode errorCode
			) throws Exception {
		resultActions
				.andExpect(status().is(errorCode.getStatus().value()))
				.andExpect(jsonPath("$.status").value(errorCode.getStatus().value()))
				.andExpect(jsonPath("$.errorCode").value(errorCode.name()))
				.andExpect(jsonPath("$.message").value(errorCode.getMessage()));
	}

	private Map<String, Object> validPlanRequest() {
		return Map.of(
				"title", "Busan tour",
				"beginDate", "2026-06-12",
				"endDate", "2026-06-13",
				"memo", "Busan memo"
				);
	}

	private Map<String, Object> planDetailsRequest(final List<Map<String, Object>> details) {
		return Map.of("details", details);
	}

	private Map<String, Object> sceneDetail(
			final Integer detailId,
			final int dayNo,
			final String beginTime,
			final int sceneId
			) {
		Map<String, Object> detail = new HashMap<>();
		if (detailId != null)
			detail.put("detailId", detailId);
		detail.put("dayNo", dayNo);
		detail.put("beginTime", beginTime);
		detail.put("sceneId", sceneId);
		return detail;
	}

	private Map<String, Object> placeDetail(
			final Integer detailId,
			final int dayNo,
			final String beginTime,
			final int placeId
			) {
		Map<String, Object> detail = new HashMap<>();
		if (detailId != null)
			detail.put("detailId", detailId);
		detail.put("dayNo", dayNo);
		detail.put("beginTime", beginTime);
		detail.put("placeId", placeId);
		return detail;
	}

	private Map<String, Object> bothTargetDetail(
			final int dayNo,
			final String beginTime,
			final int placeId,
			final int sceneId
			) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("dayNo", dayNo);
		detail.put("beginTime", beginTime);
		detail.put("placeId", placeId);
		detail.put("sceneId", sceneId);
		return detail;
	}

	private Map<String, Object> noTargetDetail(
			final int dayNo,
			final String beginTime
			) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("dayNo", dayNo);
		detail.put("beginTime", beginTime);
		return detail;
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

	private int countPlans(int memberId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM TravelPlan "
						+ "WHERE member_id = ?",
				Integer.class,
				memberId
				);
	}

	private int countPlanByPlanId(int planId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM TravelPlan "
						+ "WHERE plan_id = ?",
				Integer.class,
				planId
				);
	}

	private int countPlanDetails(int planId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) "
						+ "FROM PlanDetail "
						+ "WHERE plan_id = ?",
				Integer.class,
				planId
				);
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
			ps.setString(1, String.valueOf(nextPositiveId()));
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

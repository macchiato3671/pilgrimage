package com.ssafy.pilgrimage.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pilgrimage.model.dto.MemberDto;
import com.ssafy.pilgrimage.model.type.MemberRole;
import com.ssafy.pilgrimage.model.type.MemberStatus;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JWTFilterIntegrationTest.ProtectedTestController.class)
class JWTFilterIntegrationTest {

	private static final String PROTECTED_PATH = "/api/v1/test/protected";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@Test
	void accessToken이면_보호된_API에_접근한다() throws Exception {
		MemberDto member = testMember();
		String accessToken = jwtUtil.createAccessToken(member);

		mockMvc.perform(get(PROTECTED_PATH)
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(String.valueOf(member.getMemberId())))
				.andExpect(jsonPath("$.authority").value("ROLE_USER"));
	}

	@Test
	void 토큰이_없으면_보호된_API에_접근할_수_없다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
				.andExpect(jsonPath("$.message").value("Authentication is required."));
	}

	@Test
	void 잘못된_JWT이면_INVALID_TOKEN을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH)
						.header("Authorization", "Bearer invalid.jwt.token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
				.andExpect(jsonPath("$.message").value("Invalid token."));
	}

	@Test
	void refreshToken으로는_보호된_API에_접근할_수_없다() throws Exception {
		String refreshToken = jwtUtil.createRefreshToken(testMember());

		mockMvc.perform(get(PROTECTED_PATH)
						.header("Authorization", "Bearer " + refreshToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
				.andExpect(jsonPath("$.message").value("Invalid token."));
	}

	private MemberDto testMember() {
		MemberDto member = new MemberDto();
		member.setMemberId(1);
		member.setEmail("jwt-filter@example.com");
		member.setNickname("JWT tester");
		member.setRoleId(MemberRole.USER.getId());
		member.setStatusId(MemberStatus.ACTIVE.getId());
		return member;
	}

	@RestController
	static class ProtectedTestController {

		@GetMapping(PROTECTED_PATH)
		Map<String, String> protectedEndpoint(Authentication authentication) {
			return Map.of(
					"memberId", authentication.getName(),
					"authority", authentication.getAuthorities().iterator().next().getAuthority()
			);
		}
	}
}

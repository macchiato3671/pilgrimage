package com.moonback.pilgrimage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.AuthErrorCode;
import com.moonback.pilgrimage.model.dto.request.LoginRequestDto;
import com.moonback.pilgrimage.model.dto.response.LoginResponseDto;
import com.moonback.pilgrimage.model.dto.response.MemberResponseDto;
import com.moonback.pilgrimage.model.service.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockitoBean
	private AuthService authService;
	
	@Test
    void 로그인_성공() throws Exception {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("1234");
		
		MemberResponseDto member = MemberResponseDto.builder()
				.memberId(1)
				.email("test@example.com")
                .createdAt(LocalDateTime.of(2026, 6, 20, 15, 30))
                .nickname("테스트")
                .role("USER")
                .status("ACTIVE")
                .build();
		
		LoginResponseDto response = LoginResponseDto.builder()
				.tokenType("Bearer")
                .accessToken("temporary-access-token")
                .refreshToken("temporary-refresh-token")
                .expiresIn(3600)
                .member(member)
                .build();
		
		when(authService.login(any(LoginRequestDto.class))).thenReturn(response);
		
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("MediaType.APPLICATION_JSON")
						.content(objectMapper.writeValueAsString(response)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("temporary-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("temporary-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.member.memberId").value(1))
                .andExpect(jsonPath("$.member.email").value("test@example.com"))
                .andExpect(jsonPath("$.member.nickname").value("테스트"))
                .andExpect(jsonPath("$.member.role").value("USER"))
                .andExpect(jsonPath("$.member.status").value("ACTIVE"))
                .andExpect(jsonPath("$.member.createdAt").exists())
                .andExpect(jsonPath("$.member.password").doesNotExist());
	}
	
	@Test
	void 로그인_실패하면_401과_INVALID_CREDENTIALS를_반환한다() throws Exception {
	    // given
	    LoginRequestDto request = new LoginRequestDto();
	    request.setEmail("test@example.com");
	    request.setPassword("wrong-password");

	    when(authService.login(any(LoginRequestDto.class)))
	            .thenThrow(new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

	    // when & then
	    mockMvc.perform(post("/api/v1/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(request)))
	            .andExpect(status().isUnauthorized())
	            .andExpect(jsonPath("$.status").value(401))
	            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
	            .andExpect(jsonPath("$.message").value("Invalid email or password."));
	}
	
	@Test
	void 로그인_탈퇴회원이면_403과_MEMBER_ACCESS_DENIED를_반환한다() throws Exception {
	    // given
	    LoginRequestDto request = new LoginRequestDto();
	    request.setEmail("test@example.com");
	    request.setPassword("1234");

	    when(authService.login(any(LoginRequestDto.class)))
	            .thenThrow(new BusinessException(AuthErrorCode.MEMBER_ACCESS_DENIED));

	    // when & then
	    mockMvc.perform(post("/api/v1/auth/login")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(request)))
	            .andExpect(status().isForbidden())
	            .andExpect(jsonPath("$.status").value(403))
	            .andExpect(jsonPath("$.errorCode").value("MEMBER_ACCESS_DENIED"))
	            .andExpect(jsonPath("$.message").value("Member access is denied."));
	}
}

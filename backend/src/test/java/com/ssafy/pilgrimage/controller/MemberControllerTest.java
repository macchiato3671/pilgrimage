package com.ssafy.pilgrimage.controller;

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
import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.MemberErrorCode;
import com.ssafy.pilgrimage.model.dto.request.SignupRequestDto;
import com.ssafy.pilgrimage.model.dto.response.MemberResponseDto;
import com.ssafy.pilgrimage.model.service.MemberService;
import com.ssafy.pilgrimage.security.JWTUtil;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MemberControllerTest {
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private JWTUtil jwtUtil;
	
	@Test
    void 회원가입_성공() throws Exception {
		// given
        SignupRequestDto request = new SignupRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("1234");
        request.setNickname("테스트");

        MemberResponseDto response = MemberResponseDto.builder()
                .memberId(1)
                .email("test@example.com")
                .createdAt(LocalDateTime.of(2026, 6, 20, 15, 30))
                .nickname("테스트")
                .role("USER")
                .status("ACTIVE")
                .build();
        
        when(memberService.signup(any(SignupRequestDto.class))).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("테스트"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
        
	}
	
	@Test
	void 회원가입_중복이메일이면_409와_EMAIL_ALREADY_EXISTS를_반환한다() throws Exception {
	    // given
	    SignupRequestDto request = new SignupRequestDto();
	    request.setEmail("test@example.com");
	    request.setPassword("1234");
	    request.setNickname("테스트");

	    when(memberService.signup(any(SignupRequestDto.class)))
	            .thenThrow(new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS));

	    // when & then
	    mockMvc.perform(post("/api/v1/members")
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(request)))
	            .andExpect(status().isConflict())
	            .andExpect(jsonPath("$.status").value(409))
	            .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
	            .andExpect(jsonPath("$.message").value("Email already exists."));
	}
}

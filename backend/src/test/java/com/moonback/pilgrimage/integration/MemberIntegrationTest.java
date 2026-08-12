package com.moonback.pilgrimage.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonback.pilgrimage.model.dto.request.LoginRequestDto;
import com.moonback.pilgrimage.model.dto.request.PatchRequestDto;
import com.moonback.pilgrimage.model.dto.request.SignupRequestDto;
import com.moonback.pilgrimage.model.dto.request.WithdrawRequestDto;
import com.moonback.pilgrimage.support.AbstractMySqlIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
public class MemberIntegrationTest extends AbstractMySqlIntegrationTest {
	
	@Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private int memberId;

    @BeforeEach
    void setUpMember() {
		jdbcTemplate.update(
				"INSERT INTO member (email, password, nickname, role_id, status_id) VALUES (?, ?, ?, ?, ?)",
				"member@example.com",
				passwordEncoder.encode("Password123!"),
				"일반 사용자",
				1,
				1);
		memberId = jdbcTemplate.queryForObject(
				"SELECT member_id FROM member WHERE email = ?",
				Integer.class,
				"member@example.com");
	}
    
    @Test
    void 회원가입_성공() throws Exception {
        // given
        String email = "integration-" + System.currentTimeMillis() + "@example.com";
        String password = "1234";

        SignupRequestDto signupRequest = new SignupRequestDto();
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        signupRequest.setNickname("통합테스트");

        // when & then - 회원가입
        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nickname").value("통합테스트"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    
	
	@Test
	void 회원정보_조회() throws Exception {
		// given
		String email = "member@example.com";
		
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
		mockMvc.perform(get("/api/v1/me")
				.with(authentication(auth))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
	        .andExpect(jsonPath("$.memberId").exists())
	        .andExpect(jsonPath("$.email").value(email))
	        .andExpect(jsonPath("$.nickname").value("일반 사용자"))
	        .andExpect(jsonPath("$.role").value("USER"))
	        .andExpect(jsonPath("$.status").value("ACTIVE"))
	        .andExpect(jsonPath("$.createdAt").exists())
	        .andExpect(jsonPath("$.password").doesNotExist());	
	}
    
    @Test
    void 회원정보_수정() throws Exception {
    	// given
    	PatchRequestDto patchRequest = new PatchRequestDto();
    	patchRequest.setEmail("test@example.com");
    	patchRequest.setNickname("테스트 변경");
    	patchRequest.setCurrentPassword("Password123!");
    	patchRequest.setNewPassword("2");
    	
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
    	mockMvc.perform(patch("/api/v1/me")
    					.with(authentication(auth))
    					.contentType(MediaType.APPLICATION_JSON)
		    			.content(objectMapper.writeValueAsString(patchRequest)))
		    	.andExpect(status().isOk())
		        .andExpect(jsonPath("$.memberId").exists())
		        .andExpect(jsonPath("$.email").value("test@example.com"))
		        .andExpect(jsonPath("$.nickname").value("테스트 변경"))
		        .andExpect(jsonPath("$.role").value("USER"))
		        .andExpect(jsonPath("$.status").value("ACTIVE"))
		        .andExpect(jsonPath("$.createdAt").exists())
		        .andExpect(jsonPath("$.password").doesNotExist());
    }
    
    @Test
    void 회원탈퇴_성공() throws Exception {
    	// given
    	WithdrawRequestDto request = new WithdrawRequestDto();
    	request.setPassword("Password123!");
    	request.setReason(null);
    	
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
    	mockMvc.perform(delete("/api/v1/me")
    				.contentType(MediaType.APPLICATION_JSON)
    				.content(objectMapper.writeValueAsString(request)))
    			.andExpect(status().isNoContent());	
    }
}

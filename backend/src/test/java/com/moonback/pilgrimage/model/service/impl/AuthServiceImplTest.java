package com.moonback.pilgrimage.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.moonback.pilgrimage.exception.BusinessException;
import com.moonback.pilgrimage.exception.code.AuthErrorCode;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.dto.request.LoginRequestDto;
import com.moonback.pilgrimage.model.dto.response.LoginResponseDto;
import com.moonback.pilgrimage.model.mapper.MemberMapper;
import com.moonback.pilgrimage.model.type.MemberRole;
import com.moonback.pilgrimage.model.type.MemberStatus;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {
	
	@Mock
	private MemberMapper memberMapper;
	
	private PasswordEncoder passwordEncoder;
	
	private AuthServiceImpl authService;
	
	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		authService = new AuthServiceImpl(memberMapper, passwordEncoder);
	}
	
	@Test
	void 로그인_성공() {
		// given
		String rawPassword = "1234";
		String encodedPassword = passwordEncoder.encode(rawPassword);
		
		MemberDto member = new MemberDto();
		member.setMemberId(1);
		member.setEmail("test@example.com");
		member.setPassword(encodedPassword);
        member.setNickname("테스트");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.ACTIVE.getId());
        member.setCreatedAt(LocalDateTime.now());
        
        when(memberMapper.findByEmail("test@example.com")).thenReturn(member);
		
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("1234");
		
		// when
		LoginResponseDto response = authService.login(request);
		
		// then
		assertNotNull(response);
		assertEquals("Bearer", response.getTokenType());
		assertNotNull(response.getAccessToken());
		assertNotNull(response.getRefreshToken());
		assertEquals(3600, response.getExpiresIn());
		
		assertNotNull(response.getMember());
        assertEquals(1, response.getMember().getMemberId());
        assertEquals("test@example.com", response.getMember().getEmail());
        assertEquals("테스트", response.getMember().getNickname());
        assertEquals("USER", response.getMember().getRole());
        assertEquals("ACTIVE", response.getMember().getStatus());
        assertNotNull(response.getMember().getCreatedAt());
        
        verify(memberMapper).findByEmail("test@example.com");
	}
	
	@Test
	void 로그인_가입되지_않은_회원_예외() {
		// given
        when(memberMapper.findByEmail("none@example.com")).thenReturn(null);

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("none@example.com");
        request.setPassword("1234");
        
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );
        
        // then
        assertEquals(AuthErrorCode.MEMBER_ACCESS_DENIED, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getErrorCode().getStatus());
        assertEquals("Member access is denied.", exception.getErrorCode().getMessage());
        verify(memberMapper).findByEmail("none@example.com");
	}
	
	@Test
	void 로그인_비밀번호_불일치_예외() {
		// given
		String rawPassword = "1234";
		String encodedPassword = passwordEncoder.encode(rawPassword);
		
		MemberDto member = new MemberDto();
		member.setMemberId(1);
		member.setEmail("test@example.com");
		member.setPassword(encodedPassword);
        member.setNickname("테스트");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.ACTIVE.getId());
        member.setCreatedAt(LocalDateTime.now());
        
        when(memberMapper.findByEmail("test@example.com")).thenReturn(member);
		
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("0000");
		
		// when
		BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );
        
        // then
		assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getErrorCode().getStatus());
        assertEquals("Invalid email or password.", exception.getErrorCode().getMessage());
        verify(memberMapper).findByEmail("test@example.com");
	}
	
	@Test
	void 로그인_탈퇴한_회원이면_예외() {
		// given
		String rawPassword = "1234";
		String encodedPassword = passwordEncoder.encode(rawPassword);
		
		MemberDto member = new MemberDto();
		member.setMemberId(1);
		member.setEmail("test@example.com");
		member.setPassword(encodedPassword);
        member.setNickname("테스트");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.WITHDRAWN.getId());
        member.setCreatedAt(LocalDateTime.now());
        
        when(memberMapper.findByEmail("test@example.com")).thenReturn(member);
		
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("1234");
		
		// when
		BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );
        
        // then
		assertEquals(AuthErrorCode.MEMBER_ACCESS_DENIED, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getErrorCode().getStatus());
        assertEquals("Member access is denied.", exception.getErrorCode().getMessage());
        verify(memberMapper).findByEmail("test@example.com");
	}
	
	@Test
	void 로그인_정지된_회원이면_예외() {
		// given
		String rawPassword = "1234";
		String encodedPassword = passwordEncoder.encode(rawPassword);
		
		MemberDto member = new MemberDto();
		member.setMemberId(1);
		member.setEmail("test@example.com");
		member.setPassword(encodedPassword);
        member.setNickname("테스트");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.SUSPENDED.getId());
        member.setCreatedAt(LocalDateTime.now());
        
        when(memberMapper.findByEmail("test@example.com")).thenReturn(member);
		
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("1234");
		
		// when
		BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );
        
        // then
		assertEquals(AuthErrorCode.MEMBER_ACCESS_DENIED, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getErrorCode().getStatus());
        assertEquals("Member access is denied.", exception.getErrorCode().getMessage());
        verify(memberMapper).findByEmail("test@example.com");
	}
}

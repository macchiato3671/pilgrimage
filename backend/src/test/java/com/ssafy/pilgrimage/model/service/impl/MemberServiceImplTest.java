package com.ssafy.pilgrimage.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.MemberErrorCode;
import com.ssafy.pilgrimage.model.dto.MemberDto;
import com.ssafy.pilgrimage.model.dto.request.SignupRequestDto;
import com.ssafy.pilgrimage.model.dto.response.MemberResponseDto;
import com.ssafy.pilgrimage.model.mapper.MemberMapper;
import com.ssafy.pilgrimage.model.type.MemberRole;
import com.ssafy.pilgrimage.model.type.MemberStatus;

@ExtendWith(MockitoExtension.class)
public class MemberServiceImplTest {
	@Mock
	private MemberMapper memberMapper;
	
	private PasswordEncoder passwordEncoder;
	
	private MemberServiceImpl memberService;
	
	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		memberService = new MemberServiceImpl(memberMapper, passwordEncoder);
	}
	
	@Test
	void 회원가입_성공() {
		// given
		SignupRequestDto request = new SignupRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("1234");
		request.setNickname("테스트");
		
		when(memberMapper.countByEmail("test@example.com")).thenReturn(0);
		
		doAnswer(invocation -> {
			MemberDto member = invocation.getArgument(0);
			member.setMemberId(1);
			return 1;
		}).when(memberMapper).insertMember(any(MemberDto.class));
		
		MemberDto savedMember = new MemberDto();
		savedMember.setMemberId(1);
        savedMember.setEmail("test@example.com");
        savedMember.setPassword("encoded-password");
        savedMember.setNickname("테스트");
        savedMember.setRoleId(MemberRole.USER.getId());
        savedMember.setStatusId(MemberStatus.ACTIVE.getId());
        savedMember.setCreatedAt(LocalDateTime.now());
        
        // findById(1)을 호출하면 savedMember를 반환
        when(memberMapper.findById(1)).thenReturn(savedMember);
        
        // when
        MemberResponseDto response = memberService.signup(request);
        
        // then
        assertEquals(1, response.getMemberId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("테스트", response.getNickname());
        assertEquals("USER", response.getRole());
        assertEquals("ACTIVE", response.getStatus());
        assertNotNull(response.getCreatedAt());
        
        ArgumentCaptor<MemberDto> captor = ArgumentCaptor.forClass(MemberDto.class);
        
        // insertMember가 실제로 실행됐는지 확ㅇ니
        verify(memberMapper).insertMember(captor.capture());
        
        MemberDto insertedMember = captor.getValue();
        
        assertEquals("test@example.com", insertedMember.getEmail());
        assertEquals("테스트", insertedMember.getNickname());
        assertEquals(MemberRole.USER.getId(), insertedMember.getRoleId());
        assertEquals(MemberStatus.ACTIVE.getId(), insertedMember.getStatusId());

        assertNotEquals("1234", insertedMember.getPassword());
        assertTrue(passwordEncoder.matches("1234", insertedMember.getPassword()));
	}
	
	@Test
    void 회원가입_중복이메일이면_예외() {
		// given
		SignupRequestDto request = new SignupRequestDto();
		request.setEmail("test@example.com");
		request.setPassword("1234");
		request.setNickname("테스트");
		
		when(memberMapper.countByEmail("test@example.com")).thenReturn(1);
		
		// when
        BusinessException exception = assertThrows(
        		BusinessException.class,
                () -> memberService.signup(request)
        );
		
        // then
        assertEquals(MemberErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, exception.getErrorCode().getStatus());
        assertEquals("Email already exists.", exception.getErrorCode().getMessage());
        verify(memberMapper, never()).insertMember(any(MemberDto.class));
	}
}

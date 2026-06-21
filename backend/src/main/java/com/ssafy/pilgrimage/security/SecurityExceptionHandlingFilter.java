package com.ssafy.pilgrimage.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.exception.BusinessException;
import com.ssafy.pilgrimage.exception.code.AuthErrorCode;
import com.ssafy.pilgrimage.exception.code.CommonErrorCode;
import com.ssafy.pilgrimage.exception.code.ErrorCode;
import com.ssafy.pilgrimage.model.dto.response.ErrorResponseDto;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandlingFilter extends OncePerRequestFilter{
	private final ObjectMapper objectMapper;
	
	@Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            setErrorResponse(response, AuthErrorCode.EXPIRED_TOKEN);

        } catch (JwtException e) {
            setErrorResponse(response, AuthErrorCode.INVALID_TOKEN);

        } catch (BadCredentialsException e) {
            setErrorResponse(response, AuthErrorCode.INVALID_CREDENTIALS);

        } catch (AuthenticationException e) {
            setErrorResponse(response, AuthErrorCode.AUTHENTICATION_REQUIRED);

        } catch (AccessDeniedException e) {
            setErrorResponse(response, AuthErrorCode.MEMBER_ACCESS_DENIED);

        } catch (BusinessException e) {
            setErrorResponse(response, e.getErrorCode());

        } catch (Exception e) {
            setErrorResponse(response, CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void setErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponseDto errorResponse = ErrorResponseDto.from(errorCode);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

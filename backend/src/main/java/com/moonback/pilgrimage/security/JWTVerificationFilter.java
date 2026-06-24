package com.moonback.pilgrimage.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JWTVerificationFilter extends OncePerRequestFilter {
	private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_TYPE = "accessToken";

    private final JWTUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(
    		HttpServletRequest request, 
    		HttpServletResponse response, 
    		FilterChain filterChain
    ) throws ServletException, IOException {
    	
    	String token = extractToken(request);
    	
    	if(!StringUtils.hasText(token)) {
    		filterChain.doFilter(request, response);
    		return;
    	}
    	
		Claims claims = jwtUtil.getClaims(token);
		
		String tokenType = claims.get("tokenType", String.class);
		
		if(!ACCESS_TOKEN_TYPE.equals(tokenType)) {
			throw new JwtException("Invalid token type.");
		}
		
		String memberId = claims.getSubject();
        String role = claims.get("role", String.class);
        
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities);
        
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    	
    	filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
    	String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    	
    	if (!StringUtils.hasText(bearerToken)) {
    	    return null;
    	}
    	
    	if (!bearerToken.startsWith(BEARER_PREFIX)) {
    	    return null;
    	}
    	
    	return bearerToken.substring(BEARER_PREFIX.length());
    }
}

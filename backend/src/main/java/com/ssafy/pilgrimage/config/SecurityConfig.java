package com.ssafy.pilgrimage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import com.ssafy.pilgrimage.exception.code.AuthErrorCode;
import com.ssafy.pilgrimage.security.JWTVerificationFilter;
import com.ssafy.pilgrimage.security.SecurityExceptionHandlingFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final SecurityExceptionHandlingFilter securityExceptionHandlingFilter;
    private final JWTVerificationFilter jwtVerificationFilter;
    
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, authException) ->
								securityExceptionHandlingFilter.setErrorResponse(
										response,
										AuthErrorCode.AUTHENTICATION_REQUIRED
								)
						)
						.accessDeniedHandler((request, response, accessDeniedException) ->
								securityExceptionHandlingFilter.setErrorResponse(
										response,
										AuthErrorCode.MEMBER_ACCESS_DENIED
								)
						)
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/dramas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/scenes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/places/**").permitAll()
                        
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .anyRequest().authenticated()
				)
				.addFilterBefore(securityExceptionHandlingFilter, LogoutFilter.class)
                .addFilterBefore(jwtVerificationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
	
	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

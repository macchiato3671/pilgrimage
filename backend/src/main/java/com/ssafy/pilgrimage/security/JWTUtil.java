package com.ssafy.pilgrimage.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.model.dto.MemberDto;
import com.ssafy.pilgrimage.model.type.MemberRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTUtil {
	private final long accessExpMin;
	private final long refreshExpMin;
	private final SecretKey key;
	
	public JWTUtil( @Value("${jwt.access-token-expiration}") long accessExpMin,
					@Value("${jwt.refresh-token-expiration}") long refreshExpMin,
					@Value("${jwt.secret}") String secretkeyString) {
		this.accessExpMin = accessExpMin;
		this.refreshExpMin = refreshExpMin;
		
		this.key = Keys.hmacShaKeyFor(secretkeyString.getBytes());
	}
	
	public String createAccessToken(MemberDto member) {
		if(member.getRoleId() == null) {
			member.setRoleId(MemberRole.USER.getId());
		}
		return createToken(member, "accessToken", accessExpMin);
	}
	
	public String createRefreshToken(MemberDto member) {
		return createToken(member, "refreshToken", refreshExpMin);
	}
	
	public String createToken(MemberDto member, String tokenType, long expireMin) {
		Date now = new Date();
		Date expireDate = new Date(System.currentTimeMillis() + 1000 * 60 * expireMin);
		
		String jwt = Jwts.builder()
						.subject(String.valueOf(member.getMemberId()))
						.claim("tokenType", tokenType)
						.claim("email", member.getEmail())
						.claim("role", MemberRole.fromId(member.getRoleId()).name())
						.issuedAt(now)
						.expiration(expireDate)
						.signWith(key)
						.compact();
		
		return jwt;
	}
	
	public Claims getClaims(String token) {
		JwtParser parser = Jwts.parser().verifyWith(key).build();
		
		Jws<Claims> jws = parser.parseSignedClaims(token);
		
		return jws.getPayload();
	}

	public Integer getAccessTokenExpirationSeconds() {
	    return (int) (accessExpMin * 60);
	}
}

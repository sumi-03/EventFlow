package com.example.eventflow.global.jwt;

import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.global.security.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

// JWT Access/Refresh 토큰 발급 및 검증 (HS256, claim: userId, email)
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = properties.accessTokenValidity();
        this.refreshTokenValidity = properties.refreshTokenValidity();
    }

    public String createAccessToken(User user) {
        return buildToken(user, accessTokenValidity);
    }

    public String createRefreshToken(User user) {
        return buildToken(user, refreshTokenValidity);
    }

    private String buildToken(User user, long validityMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 서명 및 만료 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    // Refresh 토큰 만료 시각 (DB 저장용)
    public LocalDateTime getExpiration(String token) {
        Instant expiration = parseClaims(token).getExpiration().toInstant();
        return LocalDateTime.ofInstant(expiration, ZoneId.systemDefault());
    }

    // claim으로부터 인증 객체 복원 (stateless)
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        AuthUser principal = new AuthUser(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class)
        );
        return new UsernamePasswordAuthenticationToken(principal, token, Collections.emptyList());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

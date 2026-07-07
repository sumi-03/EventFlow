package com.example.eventflow.global.jwt;

import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.entity.UserRole;
import com.example.eventflow.global.security.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * JWT Access/Refresh 토큰 발급 및 검증.
 * HS256 대칭키 기반. Access 토큰에는 userId(subject), email, role claim 을 담는다.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

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
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 서명/만료 검증. 유효하지 않으면 false. */
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

    /** Refresh 토큰의 만료 시각을 DB 저장용으로 변환. */
    public LocalDateTime getExpiration(String token) {
        Instant expiration = parseClaims(token).getExpiration().toInstant();
        return LocalDateTime.ofInstant(expiration, ZoneId.systemDefault());
    }

    /** claim 으로부터 인증 객체 복원 (DB 조회 없이 stateless). */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        AuthUser principal = new AuthUser(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                role
        );
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

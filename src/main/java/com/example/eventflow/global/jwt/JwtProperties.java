package com.example.eventflow.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

// jwt.* 설정 매핑
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidity,
        long refreshTokenValidity
) {
}

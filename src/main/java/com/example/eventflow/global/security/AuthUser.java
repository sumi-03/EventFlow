package com.example.eventflow.global.security;

// 인증 사용자 정보 (JWT claim에서 복원되는 principal)
public record AuthUser(Long userId, String email) {
}

package com.example.eventflow.global.security;

import com.example.eventflow.domain.user.entity.UserRole;

/**
 * 인증된 사용자 정보. JWT Access Token 의 claim 으로부터 복원되어
 * SecurityContext 의 principal 로 저장된다. (@AuthenticationPrincipal 로 주입)
 */
public record AuthUser(Long userId, String email, UserRole role) {
}

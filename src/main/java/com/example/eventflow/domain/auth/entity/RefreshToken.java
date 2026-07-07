package com.example.eventflow.domain.auth.entity;

import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Refresh Token 관리 엔티티
// token 유니크 → 멀티세션 허용, 재발급 시 회전(RTR)
@Entity
@Getter
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public RefreshToken(Long userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}

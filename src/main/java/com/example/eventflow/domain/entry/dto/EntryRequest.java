package com.example.eventflow.domain.entry.dto;

import jakarta.validation.constraints.NotBlank;

// 입장 검증 요청, QR 토큰으로 티켓 확인
public record EntryRequest(
        @NotBlank String qrToken
) {
}

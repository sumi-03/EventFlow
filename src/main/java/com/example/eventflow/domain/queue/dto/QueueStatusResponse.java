package com.example.eventflow.domain.queue.dto;

// 대기열 진입/조회 응답. admitted면 예매 API 호출 가능한 permit이 함께 발급된 상태다
public record QueueStatusResponse(
        String queueToken,
        boolean admitted,
        long aheadOfYou
) {
}

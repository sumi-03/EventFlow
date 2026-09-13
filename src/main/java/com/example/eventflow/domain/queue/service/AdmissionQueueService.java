package com.example.eventflow.domain.queue.service;

import com.example.eventflow.domain.queue.dto.QueueStatusResponse;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

// 예매 API 앞단 입장 제어. Redis ZSET(도착 순서)에 등록하고,
// "대기열 시작 시각 + 경과 시간"으로 계산한 커서보다 내 순위가 앞이면 입장을 허용한다.
// 커서를 스케줄러로 증가시키지 않는 이유: ECS 태스크가 여러 개라 각 태스크가 스케줄러를
// 따로 돌리면 커서가 태스크 수만큼 빨리 늘어난다. 시간 기반 순수 계산이면 어느 태스크가
// 계산해도 같은 값이 나와서 별도 조율(분산락 등)이 필요 없다.
@Service
public class AdmissionQueueService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final long admitIntervalMillis;
    private final long admitBatchSize;
    private final Duration permitTtl;

    public AdmissionQueueService(StringRedisTemplate redisTemplate,
                                  Clock clock,
                                  @Value("${queue.admit-interval-millis:2000}") long admitIntervalMillis,
                                  @Value("${queue.admit-batch-size:50}") long admitBatchSize,
                                  @Value("${queue.permit-ttl-seconds:300}") long permitTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.admitIntervalMillis = admitIntervalMillis;
        this.admitBatchSize = admitBatchSize;
        this.permitTtl = Duration.ofSeconds(permitTtlSeconds);
    }

    public QueueStatusResponse join(Long scheduleId) {
        ensureStarted(scheduleId);
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add(waitingKey(scheduleId), token, clock.millis());
        return status(scheduleId, token);
    }

    public QueueStatusResponse status(Long scheduleId, String token) {
        Long rank = redisTemplate.opsForZSet().rank(waitingKey(scheduleId), token);
        if (rank == null) {
            throw new BusinessException(ErrorStatus.QUEUE_TOKEN_NOT_FOUND);
        }

        long cursor = admittedCursor(scheduleId);
        if (rank < cursor) {
            issuePermit(scheduleId, token);
            return new QueueStatusResponse(token, true, 0);
        }
        return new QueueStatusResponse(token, false, rank - cursor);
    }

    // 예매 서비스가 호출: 발급된 permit을 1회성으로 소비(검증과 동시에 삭제)한다
    public boolean consumePermit(Long scheduleId, String token) {
        if (token == null) {
            return false;
        }
        Boolean deleted = redisTemplate.delete(permitKey(scheduleId, token));
        return Boolean.TRUE.equals(deleted);
    }

    // 이 회차가 한 번이라도 대기열을 거친 적 있는지 - 없으면 예매 시 permit을 요구하지 않는다
    public boolean isQueueActive(Long scheduleId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(startedAtKey(scheduleId)));
    }

    private void issuePermit(Long scheduleId, String token) {
        redisTemplate.opsForValue().setIfAbsent(permitKey(scheduleId, token), "1", permitTtl);
    }

    private long admittedCursor(Long scheduleId) {
        String startedAt = redisTemplate.opsForValue().get(startedAtKey(scheduleId));
        if (startedAt == null) {
            return 0;
        }
        long elapsed = clock.millis() - Long.parseLong(startedAt);
        long ticks = Math.max(0, elapsed / admitIntervalMillis);
        return ticks * admitBatchSize;
    }

    private void ensureStarted(Long scheduleId) {
        redisTemplate.opsForValue().setIfAbsent(startedAtKey(scheduleId), String.valueOf(clock.millis()));
    }

    private String waitingKey(Long scheduleId) {
        return "queue:%d:waiting".formatted(scheduleId);
    }

    private String startedAtKey(Long scheduleId) {
        return "queue:%d:started-at".formatted(scheduleId);
    }

    private String permitKey(Long scheduleId, String token) {
        return "queue:%d:permit:%s".formatted(scheduleId, token);
    }
}

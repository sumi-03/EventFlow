package com.example.eventflow.domain.queue.controller;

import com.example.eventflow.domain.queue.dto.QueueStatusResponse;
import com.example.eventflow.domain.queue.service.AdmissionQueueService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "대기열", description = "예매 입장 제어 API")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/queue")
public class QueueController {

    private final AdmissionQueueService admissionQueueService;

    public QueueController(AdmissionQueueService admissionQueueService) {
        this.admissionQueueService = admissionQueueService;
    }

    @Operation(summary = "대기열 진입", description = "대기열에 등록하고 대기 토큰을 발급받습니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<QueueStatusResponse>> join(@PathVariable Long scheduleId) {
        QueueStatusResponse response = admissionQueueService.join(scheduleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.QUEUE_JOINED, response));
    }

    @Operation(summary = "대기열 순번 조회", description = "내 순번과 입장 허용 여부를 조회합니다.")
    @GetMapping("/{queueToken}")
    public CommonResponse<QueueStatusResponse> status(@PathVariable Long scheduleId,
                                                        @PathVariable String queueToken) {
        return CommonResponse.onSuccess(admissionQueueService.status(scheduleId, queueToken));
    }
}

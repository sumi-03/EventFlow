package com.example.eventflow.global.payload.status;

// 성공/실패 상태 enum 공통 인터페이스
public interface BaseStatus {

    ReasonDto getReasonHttpStatus();
}

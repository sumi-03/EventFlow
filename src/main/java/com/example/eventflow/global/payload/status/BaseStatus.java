package com.example.eventflow.global.payload.status;

/**
 * 성공/실패 상태 enum 이 공통으로 구현하는 인터페이스.
 * CommonResponse 와 전역 예외 처리에서 코드/메시지/HTTP 상태를 꺼내 쓴다.
 */
public interface BaseStatus {

    ReasonDto getReasonHttpStatus();
}

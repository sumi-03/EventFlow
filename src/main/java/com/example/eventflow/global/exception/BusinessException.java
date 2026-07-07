package com.example.eventflow.global.exception;

import com.example.eventflow.global.payload.status.BaseStatus;
import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외. 상태(BaseStatus)를 담아
 * 전역 예외 처리기가 코드/메시지/HTTP 상태를 일관되게 응답하도록 한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BaseStatus status;

    public BusinessException(BaseStatus status) {
        super(status.getReasonHttpStatus().getMessage());
        this.status = status;
    }
}

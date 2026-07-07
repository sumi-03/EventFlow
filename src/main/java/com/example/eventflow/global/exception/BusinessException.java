package com.example.eventflow.global.exception;

import com.example.eventflow.global.payload.status.BaseStatus;
import lombok.Getter;

// 상태(BaseStatus)를 담는 비즈니스 예외
@Getter
public class BusinessException extends RuntimeException {

    private final BaseStatus status;

    public BusinessException(BaseStatus status) {
        super(status.getReasonHttpStatus().getMessage());
        this.status = status;
    }
}

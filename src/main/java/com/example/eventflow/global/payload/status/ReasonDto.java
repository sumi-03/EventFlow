package com.example.eventflow.global.payload.status;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 상태(Status) enum 이 가진 코드/메시지/HTTP 상태를 담아 전달하는 값 객체.
 */
@Getter
@Builder
public class ReasonDto {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

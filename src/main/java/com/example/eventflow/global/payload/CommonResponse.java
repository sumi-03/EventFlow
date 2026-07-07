package com.example.eventflow.global.payload;

import com.example.eventflow.global.payload.status.BaseStatus;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 모든 API 응답 공통 포맷
// result가 null이면 생략, httpStatus는 직렬화 제외
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class CommonResponse<T> {

    @Getter(AccessLevel.NONE)
    @JsonProperty("isSuccess")
    private final boolean isSuccess;

    private final String code;

    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    @JsonIgnore
    private final HttpStatus httpStatus;

    // 성공 (기본 200)
    public static <T> CommonResponse<T> onSuccess(T result) {
        return new CommonResponse<>(true,
                SuccessStatus.OK.getCode(),
                SuccessStatus.OK.getMessage(),
                result,
                SuccessStatus.OK.getHttpStatus());
    }

    // 성공 (상태 지정)
    public static <T> CommonResponse<T> of(BaseStatus status, T result) {
        return new CommonResponse<>(true,
                status.getReasonHttpStatus().getCode(),
                status.getReasonHttpStatus().getMessage(),
                result,
                status.getReasonHttpStatus().getHttpStatus());
    }

    // 전역 예외 처리에서 사용
    public static <T> CommonResponse<T> onFailure(String code, String message, T result) {
        return new CommonResponse<>(false, code, message, result, null);
    }
}

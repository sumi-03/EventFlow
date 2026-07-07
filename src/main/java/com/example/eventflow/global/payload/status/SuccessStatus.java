package com.example.eventflow.global.payload.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {

    // 공통
    OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),

    // 인증
    SIGNUP_SUCCESS(HttpStatus.CREATED, "AUTH201", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH2001", "로그인 되었습니다."),
    REISSUE_SUCCESS(HttpStatus.OK, "AUTH2002", "토큰이 재발급되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH2003", "로그아웃 되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}

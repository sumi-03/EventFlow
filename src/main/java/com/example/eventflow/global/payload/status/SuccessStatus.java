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

    // 행사
    EVENT_CREATED(HttpStatus.CREATED, "EVENT201", "행사가 생성되었습니다."),
    EVENT_UPDATED(HttpStatus.OK, "EVENT2001", "행사가 수정되었습니다."),
    EVENT_CLOSED(HttpStatus.OK, "EVENT2002", "행사가 마감되었습니다."),
    EVENT_DELETED(HttpStatus.OK, "EVENT2003", "행사가 삭제되었습니다."),

    // 회차/좌석
    SCHEDULE_CREATED(HttpStatus.CREATED, "SCHEDULE201", "회차가 생성되었습니다."),
    SEAT_REGISTERED(HttpStatus.CREATED, "SEAT201", "좌석이 등록되었습니다."),

    // 예매
    RESERVATION_CREATED(HttpStatus.CREATED, "RESERVATION201", "예매가 완료되었습니다."),
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

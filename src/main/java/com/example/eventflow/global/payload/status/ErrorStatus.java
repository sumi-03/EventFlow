package com.example.eventflow.global.payload.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {

    // 공통
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 오류가 발생했습니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),

    // 인증/회원
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "AUTH409", "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4010", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4011", "유효하지 않은 Refresh Token 입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH4012", "만료되었거나 이미 사용된 Refresh Token 입니다."),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH4013", "존재하지 않는 사용자입니다."),

    // 행사
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404", "존재하지 않는 행사입니다."),
    EVENT_FORBIDDEN(HttpStatus.FORBIDDEN, "EVENT403", "행사에 대한 권한이 없습니다."),
    INVALID_EVENT_PERIOD(HttpStatus.BAD_REQUEST, "EVENT400", "행사 종료 시각은 시작 시각 이후여야 합니다."),

    // 회차
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE404", "존재하지 않는 회차입니다."),
    INVALID_SCHEDULE_PERIOD(HttpStatus.BAD_REQUEST, "SCHEDULE400", "회차 일정이 올바르지 않습니다."),

    // 좌석
    INVALID_SEAT_RANGE(HttpStatus.BAD_REQUEST, "SEAT400", "좌석 번호 범위가 올바르지 않습니다."),
    DUPLICATE_SEAT(HttpStatus.CONFLICT, "SEAT409", "이미 등록된 좌석 번호가 있습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT404", "존재하지 않는 좌석입니다."),

    // 예매
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "RESERVATION409", "이미 예매된 좌석입니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION404", "존재하지 않는 예매입니다."),
    RESERVATION_FORBIDDEN(HttpStatus.FORBIDDEN, "RESERVATION403", "예매에 대한 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}

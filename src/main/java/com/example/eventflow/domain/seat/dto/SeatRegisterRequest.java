package com.example.eventflow.domain.seat.dto;

import com.example.eventflow.domain.seat.entity.SeatGrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

// 좌석 일괄 등록 요청, 구역(그룹)별 rowPrefix+startNumber~endNumber 좌석 생성
// 예) {grade:VIP, price:100000, rowPrefix:"A", startNumber:1, endNumber:50} → A1~A50 (50석)
public record SeatRegisterRequest(
        @NotEmpty @Valid List<SeatGroup> groups
) {
    public record SeatGroup(
            @NotNull SeatGrade grade,
            @NotNull @Positive Integer price,
            @NotNull String rowPrefix,
            @NotNull @Positive Integer startNumber,
            @NotNull @Positive Integer endNumber
    ) {
    }
}

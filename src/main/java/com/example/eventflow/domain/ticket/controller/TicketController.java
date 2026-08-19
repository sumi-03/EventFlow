package com.example.eventflow.domain.ticket.controller;

import com.example.eventflow.domain.ticket.dto.TicketResponse;
import com.example.eventflow.domain.ticket.service.TicketService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "티켓", description = "티켓 조회 API")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "내 티켓 목록 조회")
    @GetMapping
    public CommonResponse<List<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal AuthUser authUser) {
        return CommonResponse.onSuccess(ticketService.getMyTickets(authUser.userId()));
    }

    @Operation(summary = "티켓 상세 조회", description = "QR 토큰을 포함한 티켓 정보를 조회합니다.")
    @GetMapping("/{ticketId}")
    public CommonResponse<TicketResponse> getTicket(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long ticketId) {
        return CommonResponse.onSuccess(ticketService.getTicket(authUser.userId(), ticketId));
    }
}

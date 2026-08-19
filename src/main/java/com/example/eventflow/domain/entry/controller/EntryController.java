package com.example.eventflow.domain.entry.controller;

import com.example.eventflow.domain.entry.dto.EntryRequest;
import com.example.eventflow.domain.entry.dto.EntryResponse;
import com.example.eventflow.domain.entry.service.EntryService;
import com.example.eventflow.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "입장", description = "입장 검증 API")
@RestController
@RequestMapping("/api/entries")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @Operation(summary = "입장 검증", description = "QR 토큰으로 티켓을 확인해 입장 처리합니다.")
    @PostMapping
    public CommonResponse<EntryResponse> verify(@Valid @RequestBody EntryRequest request) {
        return CommonResponse.onSuccess(entryService.verify(request));
    }
}

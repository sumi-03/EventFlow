package com.example.eventflow.domain.auth.controller;

import com.example.eventflow.domain.auth.dto.*;
import com.example.eventflow.domain.auth.service.AuthService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입/로그인/토큰 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.SIGNUP_SUCCESS, authService.signup(request)));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/login")
    public CommonResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return CommonResponse.of(SuccessStatus.LOGIN_SUCCESS, authService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh 토큰을 검증하고 Access/Refresh 토큰을 새로 발급합니다.")
    @PostMapping("/reissue")
    public CommonResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return CommonResponse.of(SuccessStatus.REISSUE_SUCCESS, authService.reissue(request));
    }

    @Operation(summary = "로그아웃", description = "Refresh 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public CommonResponse<Void> logout(@Valid @RequestBody ReissueRequest request) {
        authService.logout(request);
        return CommonResponse.of(SuccessStatus.LOGOUT_SUCCESS, null);
    }
}

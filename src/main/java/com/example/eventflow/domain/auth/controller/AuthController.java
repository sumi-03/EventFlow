package com.example.eventflow.domain.auth.controller;

import com.example.eventflow.domain.auth.dto.*;
import com.example.eventflow.domain.auth.service.AuthService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.SIGNUP_SUCCESS, authService.signup(request)));
    }

    @PostMapping("/login")
    public CommonResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return CommonResponse.of(SuccessStatus.LOGIN_SUCCESS, authService.login(request));
    }

    @PostMapping("/reissue")
    public CommonResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return CommonResponse.of(SuccessStatus.REISSUE_SUCCESS, authService.reissue(request));
    }

    @PostMapping("/logout")
    public CommonResponse<Void> logout(@Valid @RequestBody ReissueRequest request) {
        authService.logout(request);
        return CommonResponse.of(SuccessStatus.LOGOUT_SUCCESS, null);
    }
}

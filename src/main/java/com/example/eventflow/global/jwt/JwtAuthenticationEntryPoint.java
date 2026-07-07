package com.example.eventflow.global.jwt;

import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 미인증 요청에 401 CommonResponse 응답
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorStatus status = ErrorStatus._UNAUTHORIZED;
        response.setStatus(status.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CommonResponse<Void> body =
                CommonResponse.onFailure(status.getCode(), status.getMessage(), null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

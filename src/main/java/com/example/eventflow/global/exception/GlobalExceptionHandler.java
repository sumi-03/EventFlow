package com.example.eventflow.global.exception;

import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.ErrorStatus;
import com.example.eventflow.global.payload.status.ReasonDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusiness(BusinessException e) {
        ReasonDto reason = e.getStatus().getReasonHttpStatus();
        return ResponseEntity.status(reason.getHttpStatus())
                .body(CommonResponse.onFailure(reason.getCode(), reason.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse(ErrorStatus._BAD_REQUEST.getMessage());
        return ResponseEntity.status(ErrorStatus._BAD_REQUEST.getHttpStatus())
                .body(CommonResponse.onFailure(ErrorStatus._BAD_REQUEST.getCode(), message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        return ResponseEntity.status(ErrorStatus._INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(CommonResponse.onFailure(
                        ErrorStatus._INTERNAL_SERVER_ERROR.getCode(),
                        ErrorStatus._INTERNAL_SERVER_ERROR.getMessage(), null));
    }
}

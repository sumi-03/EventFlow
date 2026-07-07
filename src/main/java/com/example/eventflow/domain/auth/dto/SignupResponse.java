package com.example.eventflow.domain.auth.dto;

import com.example.eventflow.domain.user.entity.User;

public record SignupResponse(Long id, String email, String name) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getName());
    }
}

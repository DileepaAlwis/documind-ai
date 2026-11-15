package com.documind.user;

import java.time.Instant;
import java.time.ZoneOffset;

public record UserResponse(Long id, String email, String fullName, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
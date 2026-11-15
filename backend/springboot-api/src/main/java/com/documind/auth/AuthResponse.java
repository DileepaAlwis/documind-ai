package com.documind.auth;

import com.documind.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}

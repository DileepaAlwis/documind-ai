package com.documind.auth;

/** The JSON body for the client plus the raw refresh token that goes into the cookie. */
public record AuthTokens(AuthResponse response, String refreshToken) {
}

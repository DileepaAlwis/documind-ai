package com.documind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cookie cookie, Cors cors) {

    public record Jwt(String secret, long accessTokenMinutes, long refreshTokenDays) {
    }

    public record Cookie(boolean secure) {
    }

    public record Cors(String allowedOrigin) {
    }
}

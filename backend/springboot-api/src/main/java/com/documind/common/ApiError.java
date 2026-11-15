package com.documind.common;

public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
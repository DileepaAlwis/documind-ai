package com.documind.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the caller's identity from the verified token. Never trust an ID sent in a request. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        if (jwt == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user is available.");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("JWT subject must be a numeric user ID.");
        }

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException("JWT subject must be a numeric user ID.", ex);
        }
    }
}

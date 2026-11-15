package com.documind.auth;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.documind.common.ApiException;
import com.documind.config.AppProperties;
import com.documind.security.JwtService;
import com.documind.security.TokenUtil;
import com.documind.user.User;
import com.documind.user.UserRepository;
import com.documind.user.UserResponse;
import com.documind.user.UserStatus;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AppProperties props) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
        this.dummyHash = passwordEncoder.encode("not-a-real-password-1");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw emailTaken();   // two registrations with the same email at the same moment
        }
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> found = users.findByEmail(email);

        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordOk) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        User user = found.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is disabled.");
        }
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokens refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }
        RefreshToken stored = refreshTokens.findByTokenHash(TokenUtil.sha256Hex(rawToken))
                .orElseThrow(this::invalidRefreshToken);
        User user = stored.getUser();

        if (stored.isRevoked()) {
            // A rotated token was used again: assume theft and end every session of this user.
            refreshTokens.revokeAllForUser(user.getId());
            throw invalidRefreshToken();
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw invalidRefreshToken();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is disabled.");
        }
        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(TokenUtil.sha256Hex(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private AuthTokens issueTokens(User user) {
        String rawRefreshToken = TokenUtil.newRefreshToken();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(TokenUtil.sha256Hex(rawRefreshToken));
        entity.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(props.jwt().refreshTokenDays()));
        refreshTokens.save(entity);

        AuthResponse response = new AuthResponse(
                jwtService.generateAccessToken(user),
                "Bearer",
                jwtService.accessTokenSeconds(),
                UserResponse.from(user)
        );
        return new AuthTokens(response, rawRefreshToken);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "This email is already registered.");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token.");
    }
}

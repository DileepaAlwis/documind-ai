package com.documind.auth;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.documind.security.TokenUtil;
import com.documind.support.AbstractIntegrationTest;

class AuthIntegrationTest extends AbstractIntegrationTest {

    // ---------------- registration ----------------

    @Test
    void register_createsUserWithoutExposingPassword() throws Exception {
        postJson("/api/auth/register", registerBody(EMAIL, PASSWORD, "Alice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_storesBcryptHashNotThePassword() throws Exception {
        register(EMAIL);
        String stored = users.findByEmail(EMAIL).orElseThrow().getPasswordHash();
        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void register_duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register(EMAIL);
        postJson("/api/auth/register", registerBody("ALICE@Example.com", PASSWORD, "Other"))
                .andExpect(status().isConflict());
    }

    @Test
    void register_weakPasswordIsRejected() throws Exception {
        postJson("/api/auth/register", registerBody("weak@example.com", "abc", "Weak"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("password")));
    }

    @Test
    void register_invalidEmailAndEmptyBodyAreRejected() throws Exception {
        postJson("/api/auth/register", registerBody("not-an-email", PASSWORD, "Bad"))
                .andExpect(status().isBadRequest());
        postJson("/api/auth/register", "{}")
                .andExpect(status().isBadRequest());
    }

    // ---------------- login ----------------

    @Test
    void login_returnsAccessTokenAndHardenedRefreshCookie() throws Exception {
        register(EMAIL);
        MvcResult result = postJson("/api/auth/login", loginBody(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("refresh_token=", "HttpOnly", "Path=/api/auth", "SameSite=Strict");
    }

    @Test
    void login_isCaseInsensitiveForEmail() throws Exception {
        register(EMAIL);
        postJson("/api/auth/login", loginBody("ALICE@EXAMPLE.COM", PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void login_wrongPasswordAndUnknownEmailGiveTheSameResponse() throws Exception {
        register(EMAIL);
        postJson("/api/auth/login", loginBody(EMAIL, "WrongPass999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        postJson("/api/auth/login", loginBody("nobody@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void login_disabledAccountIsForbidden() throws Exception {
        register(EMAIL);
        disable(EMAIL);
        postJson("/api/auth/login", loginBody(EMAIL, PASSWORD))
                .andExpect(status().isForbidden());
    }

    // ---------------- refresh ----------------

    @Test
    void refresh_rotatesTheTokenAndReturnsANewAccessToken() throws Exception {
        register(EMAIL);
        String first = refreshToken(login(EMAIL));

        MvcResult result = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        assertThat(refreshToken(result)).isNotEqualTo(first);
    }

    @Test
    void refresh_withoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_storesOnlyTheHashOfTheToken() throws Exception {
        register(EMAIL);
        String raw = refreshToken(login(EMAIL));

        var stored = refreshTokens.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getTokenHash())
                .isNotEqualTo(raw)
                .isEqualTo(TokenUtil.sha256Hex(raw))
                .hasSize(64);
    }

    @Test
    void refresh_reusingARotatedTokenRevokesEverySession() throws Exception {
        register(EMAIL);
        String t1 = refreshToken(login(EMAIL));
        String t2 = refreshToken(refresh(t1).andExpect(status().isOk()).andReturn());

        refresh(t1).andExpect(status().isUnauthorized());   // replay of the old token
        refresh(t2).andExpect(status().isUnauthorized());   // the newer one is now dead too
    }

    @Test
    void refresh_expiredStoredTokenIsRejected() throws Exception {
        register(EMAIL);
        String raw = refreshToken(login(EMAIL));

        RefreshToken stored = refreshTokens.findAll().get(0);
        stored.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        refreshTokens.save(stored);

        refresh(raw).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_disabledAccountIsForbidden() throws Exception {
        register(EMAIL);
        String raw = refreshToken(login(EMAIL));
        disable(EMAIL);
        refresh(raw).andExpect(status().isForbidden());
    }

    @Test
    void refresh_ignoresAnInvalidBearerHeader() throws Exception {
        // The frontend may still hold an expired access token when it asks for a new one.
        register(EMAIL);
        String raw = refreshToken(login(EMAIL));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", raw))
                        .header("Authorization", "Bearer this.is.not-valid"))
                .andExpect(status().isOk());
    }

    // ---------------- logout ----------------

    @Test
    void logout_revokesTheRefreshTokenAndClearsTheCookie() throws Exception {
        register(EMAIL);
        String raw = refreshToken(login(EMAIL));

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", raw)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
        refresh(raw).andExpect(status().isUnauthorized());
    }
}
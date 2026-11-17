package com.documind.user;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.documind.support.AbstractIntegrationTest;

class UserAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    private String loggedInToken() throws Exception {
        register(EMAIL);
        return accessToken(login(EMAIL));
    }

    // ---------------- public vs protected ----------------

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void protectedEndpoint_withoutTokenReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void unknownUrl_isUnauthorizedWhenLoggedOutAndNotFoundWhenLoggedIn() throws Exception {
        mockMvc.perform(get("/api/nope")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/nope").header("Authorization", bearer(loggedInToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------------- token validation ----------------

    @Test
    void me_withValidTokenReturnsProfileWithoutSecrets() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(loggedInToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void me_withTamperedTokenIsRejected() throws Exception {
        String token = loggedInToken();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withGarbageTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withNonNumericSubjectReturnsUnauthorized() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("documind-api")
                .subject("not-a-user-id")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "USER")
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_withExpiredTokenIsRejected() throws Exception {
        register(EMAIL);
        Long id = users.findByEmail(EMAIL).orElseThrow().getId();

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("documind-api")
                .subject(String.valueOf(id))
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .claim("role", "USER")
                .build();
        String expired = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- profile ----------------

    @Test
    void updateProfile_changesAndTrimsTheName() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(loggedInToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  New Name  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void updateProfile_blankNameIsRejected() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(loggedInToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------- roles ----------------

    @Test
    void adminArea_isForbiddenForNormalUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(loggedInToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminArea_passesTheRoleCheckForAdmins() throws Exception {
        register(EMAIL);
        promoteToAdmin(EMAIL);
        String adminToken = accessToken(login(EMAIL));   // the role is read from the new token

        // 404, not 403: the role check passed, and there is simply no admin endpoint yet.
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }
}
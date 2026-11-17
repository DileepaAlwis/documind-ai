package com.documind.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.documind.auth.RefreshTokenRepository;
import com.documind.user.Role;
import com.documind.user.User;
import com.documind.user.UserRepository;
import com.documind.user.UserStatus;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String EMAIL = "alice@example.com";
    protected static final String PASSWORD = "Passw0rd123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository users;
    @Autowired protected RefreshTokenRepository refreshTokens;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @BeforeEach
    void cleanDatabase() {
        if (!datasourceUrl.contains("/documind_test")) {
            throw new IllegalStateException("Refusing to clean a non-test database: " + datasourceUrl);
        }
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    // ---------- request helpers ----------

    protected static String registerBody(String email, String password, String fullName) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\"}".formatted(email, password, fullName);
    }

    protected static String loginBody(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected void register(String email) throws Exception {
        postJson("/api/auth/register", registerBody(email, PASSWORD, "Test User"))
                .andExpect(status().isCreated());
    }

    protected MvcResult login(String email) throws Exception {
        return postJson("/api/auth/login", loginBody(email, PASSWORD))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refreshToken)));
    }

    // ---------- reading results ----------

    protected static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    /** Pulls the raw refresh token out of the Set-Cookie header. */
    protected static String refreshToken(MvcResult result) {
        String header = result.getResponse().getHeader("Set-Cookie");
        return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- data helpers ----------

    protected void promoteToAdmin(String email) {
        User user = users.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        users.save(user);
    }

    protected void disable(String email) {
        User user = users.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.DISABLED);
        users.save(user);
    }

    protected String registerAndLogin(String email) throws Exception {
        register(email);
        return accessToken(login(email));
    }
}

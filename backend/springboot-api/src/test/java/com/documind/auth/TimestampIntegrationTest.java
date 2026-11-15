package com.documind.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.documind.support.AbstractIntegrationTest;

/** Regression test: timestamps must be stored as UTC, whatever the machine's time zone is. */
class TimestampIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    private long queryLong(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    void userCreatedAt_isStoredInUtc() throws Exception {
        register(EMAIL);
        long secondsAgo = queryLong("SELECT TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) FROM users");
        assertThat(secondsAgo).isBetween(-5L, 60L);
    }

    @Test
    void refreshTokenExpiry_isAboutSevenDaysAheadInUtc() throws Exception {
        // Assumes the default JWT_REFRESH_TOKEN_DAYS of 7. Change the range if you override it in .env.
        register(EMAIL);
        login(EMAIL);
        long hoursAhead = queryLong("SELECT TIMESTAMPDIFF(HOUR, UTC_TIMESTAMP(), expires_at) FROM refresh_tokens");
        assertThat(hoursAhead).isBetween(166L, 168L);
    }
}
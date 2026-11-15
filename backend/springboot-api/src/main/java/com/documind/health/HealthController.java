package com.documind.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp;
        try (Connection connection = dataSource.getConnection()) {
            dbUp = connection.isValid(2);
        } catch (SQLException e) {
            dbUp = false;
        }
        Map<String, Object> body = Map.of(
                "status", dbUp ? "UP" : "DEGRADED",
                "service", "documind-api",
                "database", dbUp ? "UP" : "DOWN",
                "time", Instant.now().toString()
        );
        return dbUp ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }
}

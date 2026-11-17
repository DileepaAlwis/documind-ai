package com.documind.workspace;

import com.documind.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceIntegrationTest extends AbstractIntegrationTest {

    private static final String ALICE = "alice@example.com";
    private static final String BOB = "bob@example.com";

    @Autowired
    private DataSource dataSource;

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void createTwoUsers() throws Exception {
        aliceToken = registerAndLogin(ALICE);
        bobToken = registerAndLogin(BOB);
    }

    // ---------------- helpers ----------------

    private ResultActions create(String token, String name, String description) throws Exception {
        String body = description == null
                ? "{\"name\":\"%s\"}".formatted(name)
                : "{\"name\":\"%s\",\"description\":\"%s\"}".formatted(name, description);
        return mockMvc.perform(post("/api/workspaces")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long createdId(String token, String name) throws Exception {
        String json = create(token, name, null).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions put(String token, long id, String name, String description) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/workspaces/" + id)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"description\":\"%s\"}".formatted(name, description)));
    }

    private ResultActions getOne(String token, long id) throws Exception {
        return mockMvc.perform(get("/api/workspaces/" + id).header("Authorization", bearer(token)));
    }

    private long userId(String email) {
        return users.findByEmail(email).orElseThrow().getId();
    }

    private void insertDocument(long userId, long workspaceId, String status) throws Exception {
        String sql = "INSERT INTO documents (user_id, workspace_id, original_filename, stored_filename, "
                + "file_type, file_size_bytes, content_hash, status) VALUES (?, ?, 'doc.pdf', ?, 'PDF', 1000, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, workspaceId);
            statement.setString(3, UUID.randomUUID().toString());
            statement.setString(4, "a".repeat(64));
            statement.setString(5, status);
            statement.executeUpdate();
        }
    }

    private long countDocuments() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM documents");
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    // ---------------- create ----------------

    @Test
    void create_returnsTheWorkspaceWithEmptyStatistics() throws Exception {
        create(aliceToken, "  Biology  ", "  Cell notes  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Biology"))
                .andExpect(jsonPath("$.description").value("Cell notes"))
                .andExpect(jsonPath("$.documentCount").value(0))
                .andExpect(jsonPath("$.readyDocumentCount").value(0));
    }

    @Test
    void create_rejectsBlankAndTooLongInput() throws Exception {
        create(aliceToken, "   ", null).andExpect(status().isBadRequest());
        create(aliceToken, "x".repeat(121), null).andExpect(status().isBadRequest());
        create(aliceToken, "Fine", "d".repeat(1001)).andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateNameIsAConflictForTheSameUserButAllowedForOthers() throws Exception {
        create(aliceToken, "Biology", null).andExpect(status().isCreated());
        create(aliceToken, "BIOLOGY", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A workspace with this name already exists."));
        create(bobToken, "Biology", null).andExpect(status().isCreated());
    }

    // ---------------- ownership ----------------

    @Test
    void list_returnsOnlyTheCallersWorkspaces() throws Exception {
        createdId(aliceToken, "Alice One");
        createdId(aliceToken, "Alice Two");
        createdId(bobToken, "Bob One");

        mockMvc.perform(get("/api/workspaces").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Alice One", "Alice Two")));

        mockMvc.perform(get("/api/workspaces").header("Authorization", bearer(bobToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Bob One"));
    }

    @Test
    void get_anotherUsersWorkspaceLooksExactlyLikeAMissingOne() throws Exception {
        long aliceWorkspace = createdId(aliceToken, "Private");

        getOne(bobToken, aliceWorkspace)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workspace not found."));
        getOne(bobToken, 999_999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workspace not found."));
        getOne(aliceToken, aliceWorkspace).andExpect(status().isOk());
    }

    @Test
    void update_anotherUsersWorkspaceIsNotFoundAndLeavesItUnchanged() throws Exception {
        long aliceWorkspace = createdId(aliceToken, "Original");

        put(bobToken, aliceWorkspace, "Hijacked", "x").andExpect(status().isNotFound());

        getOne(aliceToken, aliceWorkspace)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original"));
    }

    @Test
    void delete_anotherUsersWorkspaceIsNotFoundAndItStillExists() throws Exception {
        long aliceWorkspace = createdId(aliceToken, "Keep me");

        mockMvc.perform(delete("/api/workspaces/" + aliceWorkspace).header("Authorization", bearer(bobToken)))
                .andExpect(status().isNotFound());

        getOne(aliceToken, aliceWorkspace).andExpect(status().isOk());
    }

    // ---------------- update and delete by the owner ----------------

    @Test
    void update_ownerCanChangeFieldsAndRenameToTheirOwnCurrentName() throws Exception {
        long id = createdId(aliceToken, "Old name");
        createdId(aliceToken, "Taken");

        put(aliceToken, id, "New name", "New description")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.description").value("New description"));

        put(aliceToken, id, "New name", "Different description").andExpect(status().isOk());
        put(aliceToken, id, "taken", "x").andExpect(status().isConflict());
    }

    @Test
    void delete_ownerRemovesTheWorkspaceAndOnlyItsDocuments() throws Exception {
        long aliceWorkspace = createdId(aliceToken, "Doomed");
        long bobWorkspace = createdId(bobToken, "Safe");
        insertDocument(userId(ALICE), aliceWorkspace, "READY");
        insertDocument(userId(ALICE), aliceWorkspace, "FAILED");
        insertDocument(userId(BOB), bobWorkspace, "READY");

        mockMvc.perform(delete("/api/workspaces/" + aliceWorkspace).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNoContent());

        getOne(aliceToken, aliceWorkspace).andExpect(status().isNotFound());
        org.assertj.core.api.Assertions.assertThat(countDocuments()).isEqualTo(1);   // only Bob's remains
    }

    // ---------------- search and statistics ----------------

    @Test
    void search_isCaseInsensitiveScopedToTheCallerAndTreatsWildcardsLiterally() throws Exception {
        createdId(aliceToken, "Math");
        create(aliceToken, "History", "ancient rome").andExpect(status().isCreated());
        createdId(aliceToken, "100% Done");
        createdId(bobToken, "Math for Bob");

        mockMvc.perform(get("/api/workspaces").param("q", "MATH").header("Authorization", bearer(aliceToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Math"));

        mockMvc.perform(get("/api/workspaces").param("q", "rome").header("Authorization", bearer(aliceToken)))
                .andExpect(jsonPath("$[0].name").value("History"));

        // A bare "%" must match only a literal percent sign, not everything.
        mockMvc.perform(get("/api/workspaces").param("q", "%").header("Authorization", bearer(aliceToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("100% Done"));
    }

    @Test
    void statistics_countDocumentsAndReadyDocumentsPerWorkspace() throws Exception {
        long first = createdId(aliceToken, "With documents");
        long second = createdId(aliceToken, "Empty");
        insertDocument(userId(ALICE), first, "READY");
        insertDocument(userId(ALICE), first, "READY");
        insertDocument(userId(ALICE), first, "FAILED");

        getOne(aliceToken, first)
                .andExpect(jsonPath("$.documentCount").value(3))
                .andExpect(jsonPath("$.readyDocumentCount").value(2));
        getOne(aliceToken, second)
                .andExpect(jsonPath("$.documentCount").value(0))
                .andExpect(jsonPath("$.readyDocumentCount").value(0));
    }

    // ---------------- authentication and input ----------------

    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        long id = createdId(aliceToken, "Anything");

        mockMvc.perform(get("/api/workspaces")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/workspaces").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/workspaces/" + id)).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/workspaces/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/workspaces/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void get_nonNumericIdIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/workspaces/abc").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isBadRequest());
    }
}


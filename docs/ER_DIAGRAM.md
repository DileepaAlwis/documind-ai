# DocuMind AI: MySQL ER Diagram

Version: 0.1 (matches `database/schema.sql`)
Related documents: [ARCHITECTURE.md](ARCHITECTURE.md), [SRS.md](SRS.md)

Spring Boot owns this schema and applies it with Flyway migrations. FastAPI writes only to `document_chunks`.

```mermaid
erDiagram
    users ||--o{ workspaces : owns
    users ||--o{ refresh_tokens : has
    users ||--o{ activity_logs : generates
    workspaces ||--o{ documents : contains
    users ||--o{ documents : uploads
    documents ||--o{ document_chunks : split_into
    workspaces ||--o{ conversations : has
    users ||--o{ conversations : starts
    documents ||--o{ conversations : scopes
    conversations ||--o{ messages : contains
    messages ||--o{ message_sources : cites
    document_chunks ||--o{ message_sources : referenced_by
    documents ||--o{ generated_content : source_of
    users ||--o{ generated_content : requests

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar role "USER, ADMIN"
        varchar status "ACTIVE, DISABLED"
        datetime created_at
        datetime updated_at
    }
    refresh_tokens {
        bigint id PK
        bigint user_id FK
        char token_hash UK
        datetime expires_at
        boolean revoked
        datetime created_at
    }
    workspaces {
        bigint id PK
        bigint user_id FK
        varchar name "unique per user"
        text description
        datetime created_at
        datetime updated_at
    }
    documents {
        bigint id PK
        bigint user_id FK
        bigint workspace_id FK
        varchar original_filename
        varchar stored_filename UK
        varchar file_type "PDF, DOCX, TXT"
        bigint file_size_bytes
        char content_hash
        int page_count
        varchar status "UPLOADED, PROCESSING, READY, FAILED, NO_TEXT, DELETING"
        varchar error_message
        int chunk_count
        varchar embedding_model
        datetime created_at
        datetime updated_at
    }
    document_chunks {
        bigint id PK
        bigint document_id FK
        int chunk_index
        mediumtext content
        int page_start
        int page_end
        int token_count
        datetime created_at
    }
    conversations {
        bigint id PK
        bigint user_id FK
        bigint workspace_id FK
        bigint document_id FK "nullable, null = whole workspace"
        varchar title
        datetime created_at
        datetime updated_at
    }
    messages {
        bigint id PK
        bigint conversation_id FK
        varchar role "USER, ASSISTANT"
        mediumtext content
        varchar status "OK, INSUFFICIENT_EVIDENCE, FAILED"
        int latency_ms
        datetime created_at
    }
    message_sources {
        bigint id PK
        bigint message_id FK
        bigint chunk_id FK "nullable, SET NULL on chunk delete"
        varchar document_name "snapshot"
        int page_start
        int page_end
        float similarity_score
        int rank_position
    }
    generated_content {
        bigint id PK
        bigint user_id FK
        bigint document_id FK
        varchar type "SUMMARY, MCQ, QA, FLASHCARDS, NOTES"
        json content
        varchar parameters
        datetime created_at
    }
    activity_logs {
        bigint id PK
        bigint user_id FK "nullable, SET NULL on user delete"
        varchar action
        varchar entity_type
        bigint entity_id
        json details
        datetime created_at
    }
```

## Notes

- `refresh_tokens` is a tenth table added to the nine required entities, so logout and refresh-token revocation are real. Only a hash of the token is stored.
- **Key constraints:** `UNIQUE(document_id, chunk_index)` on chunks; `UNIQUE(user_id, name)` on workspaces; `UNIQUE(email)` on users; `UNIQUE(stored_filename)` on documents.
- **Cascades:** deleting a user removes their workspaces, documents and chunks; deleting a workspace removes its documents and conversations. The application also deletes files on disk and Qdrant points (see ARCHITECTURE.md, section 5), because the database cannot do that itself.
- **Indexes:** `documents(user_id, workspace_id, status)`, `documents(workspace_id, content_hash)`, `messages(conversation_id, created_at)`, `activity_logs(user_id, created_at)`, plus a `FULLTEXT` index on `document_chunks.content` for keyword search.
- `documents.content_hash` (SHA-256) detects duplicate uploads inside a workspace.
- **Conversation scope:** `document_id` set means a single document; `NULL` means the whole workspace.
- Vectors are **not** stored in MySQL. They live in Qdrant, which uses `document_chunks.id` as the point ID.
- `message_sources` stores a snapshot of document name and pages so citation history survives document deletion.

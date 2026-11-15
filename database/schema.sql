-- DocuMind AI schema (MySQL 8). Spring Boot (Flyway) owns this schema.

CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name     VARCHAR(150) NOT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT ck_users_role   CHECK (role   IN ('USER','ADMIN')),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT      NOT NULL,
  token_hash CHAR(64)    NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE workspaces (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT       NOT NULL,
  name        VARCHAR(120) NOT NULL,
  description TEXT NULL,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_workspace_user_name UNIQUE (user_id, name),
  CONSTRAINT fk_workspace_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE documents (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT       NOT NULL,
  workspace_id      BIGINT       NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename   VARCHAR(100) NOT NULL,
  file_type         VARCHAR(10)  NOT NULL,
  file_size_bytes   BIGINT       NOT NULL,
  content_hash      CHAR(64)     NOT NULL,
  page_count        INT NULL,
  status            VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
  error_message     VARCHAR(500) NULL,
  chunk_count       INT NOT NULL DEFAULT 0,
  embedding_model   VARCHAR(100) NULL,
  created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_documents_stored_filename UNIQUE (stored_filename),
  CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_documents_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
  CONSTRAINT ck_documents_type CHECK (file_type IN ('PDF','DOCX','TXT')),
  CONSTRAINT ck_documents_status CHECK (status IN
    ('UPLOADED','PROCESSING','READY','FAILED','NO_TEXT','DELETING')),
  INDEX idx_documents_owner (user_id, workspace_id, status),
  INDEX idx_documents_hash (workspace_id, content_hash)
) ENGINE=InnoDB;

CREATE TABLE document_chunks (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id  BIGINT      NOT NULL,
  chunk_index  INT         NOT NULL,
  content      MEDIUMTEXT  NOT NULL,
  page_start   INT NULL,
  page_end     INT NULL,
  token_count  INT NULL,
  created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_chunk_doc_index UNIQUE (document_id, chunk_index),
  CONSTRAINT fk_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  FULLTEXT INDEX ft_chunks_content (content)
) ENGINE=InnoDB;

CREATE TABLE conversations (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT       NOT NULL,
  workspace_id BIGINT       NOT NULL,
  document_id  BIGINT NULL,                 -- NULL = whole workspace
  title        VARCHAR(200) NOT NULL,
  created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_conv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_conv_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
  CONSTRAINT fk_conv_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  INDEX idx_conv_user (user_id, updated_at)
) ENGINE=InnoDB;

CREATE TABLE messages (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT      NOT NULL,
  role            VARCHAR(20) NOT NULL,
  content         MEDIUMTEXT  NOT NULL,
  status          VARCHAR(30) NOT NULL DEFAULT 'OK',
  latency_ms      INT NULL,
  created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_messages_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
  CONSTRAINT ck_messages_role CHECK (role IN ('USER','ASSISTANT')),
  CONSTRAINT ck_messages_status CHECK (status IN ('OK','INSUFFICIENT_EVIDENCE','FAILED')),
  INDEX idx_messages_conv (conversation_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE message_sources (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  message_id     BIGINT NOT NULL,
  chunk_id       BIGINT NULL,                -- becomes NULL if the chunk is deleted
  document_name  VARCHAR(255) NOT NULL,      -- snapshot, keeps history readable
  page_start     INT NULL,
  page_end       INT NULL,
  similarity_score FLOAT NULL,
  rank_position  INT NOT NULL,
  CONSTRAINT fk_sources_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
  CONSTRAINT fk_sources_chunk FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE SET NULL,
  INDEX idx_sources_message (message_id)
) ENGINE=InnoDB;

CREATE TABLE generated_content (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  document_id BIGINT      NOT NULL,
  type        VARCHAR(20) NOT NULL,
  content     JSON        NOT NULL,
  parameters  VARCHAR(500) NULL,
  created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_gen_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_gen_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  CONSTRAINT ck_gen_type CHECK (type IN ('SUMMARY','MCQ','QA','FLASHCARDS','NOTES')),
  INDEX idx_gen_user_doc (user_id, document_id)
) ENGINE=InnoDB;

CREATE TABLE activity_logs (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NULL,
  action      VARCHAR(60)  NOT NULL,
  entity_type VARCHAR(40)  NULL,
  entity_id   BIGINT NULL,
  details     JSON NULL,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_activity_user (user_id, created_at)
) ENGINE=InnoDB;
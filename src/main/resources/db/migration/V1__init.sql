-- pgvector extension is required by Spring AI's PgVectorStore autoconfiguration
-- (it creates its own vector_store table on startup when initialize-schema=true).
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_record (
    id            UUID PRIMARY KEY,
    filename      VARCHAR(512)  NOT NULL,
    allowed_group VARCHAR(128)  NOT NULL,
    uploaded_by   VARCHAR(128)  NOT NULL,
    uploaded_at   TIMESTAMP     NOT NULL,
    chunk_count   INTEGER       NOT NULL
);

CREATE INDEX idx_document_record_allowed_group ON document_record (allowed_group);

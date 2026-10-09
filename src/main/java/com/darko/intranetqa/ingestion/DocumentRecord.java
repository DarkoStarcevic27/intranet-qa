package com.darko.intranetqa.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadata row for an ingested document, kept in Postgres alongside the
 * vector store. This is what the "list my documents" tool and any future
 * admin UI query against — the chunks themselves live only in the vector
 * store, keyed back to this record by {@code sourceId}.
 */
@Entity
@Table(name = "document_record")
public class DocumentRecord {

    /** Assigned by the application and reused as the chunks' {@code sourceId}, so a record and its vectors can be deleted together. */
    @Id
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String allowedGroup;

    @Column(nullable = false)
    private String uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private int chunkCount;

    protected DocumentRecord() {
        // JPA
    }

    public DocumentRecord(UUID id, String filename, String allowedGroup, String uploadedBy, int chunkCount) {
        this.id = id;
        this.filename = filename;
        this.allowedGroup = allowedGroup;
        this.uploadedBy = uploadedBy;
        this.chunkCount = chunkCount;
        this.uploadedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getAllowedGroup() {
        return allowedGroup;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public int getChunkCount() {
        return chunkCount;
    }
}

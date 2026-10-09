package com.darko.intranetqa.ingestion;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Turns an uploaded file into indexed, access-controlled chunks.
 *
 * <p>Every chunk is tagged with {@code allowedGroup} so retrieval (see
 * {@link com.darko.intranetqa.security.AuthenticatedUser#filterExpression})
 * can restrict a user's search to documents their group is allowed to see.
 * A document meant for several groups is deliberately ingested once per
 * group, rather than storing a list-valued field, because the vector
 * store's filter DSL matches scalar fields, not list intersections.
 */
@Service
public class IngestionService {

    private final VectorStore vectorStore;
    private final DocumentRecordRepository documentRecordRepository;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().build();

    public IngestionService(VectorStore vectorStore, DocumentRecordRepository documentRecordRepository) {
        this.vectorStore = vectorStore;
        this.documentRecordRepository = documentRecordRepository;
    }

    public DocumentRecord ingest(MultipartFile file, String allowedGroup, String uploadedBy) {
        String filename = file.getOriginalFilename();
        if (documentRecordRepository.existsByFilenameAndAllowedGroup(filename, allowedGroup)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "'" + filename + "' is already indexed for group '" + allowedGroup + "'. Delete it first to re-upload.");
        }

        List<Document> rawDocuments = read(file);

        // The record id doubles as the chunks' sourceId, so one id links the
        // Postgres row to every vector that came from it.
        UUID id = UUID.randomUUID();
        rawDocuments.forEach(doc -> {
            doc.getMetadata().put("allowedGroup", allowedGroup);
            doc.getMetadata().put("filename", filename);
            doc.getMetadata().put("sourceId", id.toString());
        });

        List<Document> chunks = splitter.apply(rawDocuments);
        vectorStore.add(chunks);

        try {
            return documentRecordRepository.save(
                    new DocumentRecord(id, filename, allowedGroup, uploadedBy, chunks.size()));
        } catch (RuntimeException e) {
            // Don't leave orphaned vectors behind if the metadata row can't be saved.
            vectorStore.delete("sourceId == '" + id + "'");
            throw e;
        }
    }

    /** Removes the metadata row and every chunk indexed from it. */
    public void delete(DocumentRecord record) {
        vectorStore.delete("sourceId == '" + record.getId() + "'");
        documentRecordRepository.delete(record);
    }

    private List<Document> read(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".pdf")) {
                return new PagePdfDocumentReader(file.getResource()).get();
            }
            if (filename.endsWith(".md") || filename.endsWith(".markdown")) {
                var config = MarkdownDocumentReaderConfig.builder().build();
                return new MarkdownDocumentReader(file.getResource(), config).get();
            }
            // Fall back to treating anything else as plain text, one Document per file.
            String content = new String(file.getBytes());
            return List.of(new Document(content, Map.of()));
        } catch (IOException e) {
            throw new IngestionException("Could not read uploaded file: " + file.getOriginalFilename(), e);
        }
    }
}

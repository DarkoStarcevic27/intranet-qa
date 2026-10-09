package com.darko.intranetqa.web;

import com.darko.intranetqa.ingestion.DocumentRecord;
import com.darko.intranetqa.ingestion.DocumentRecordRepository;
import com.darko.intranetqa.ingestion.IngestionService;
import com.darko.intranetqa.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Document management, restricted to ROLE_UPLOADER / ROLE_ADMIN by
 * {@link com.darko.intranetqa.config.SecurityConfig}. The caller declares which
 * group the document belongs to; that becomes the access-control tag every
 * retrieval is filtered against. Non-admins may only manage documents in
 * groups they themselves belong to.
 */
@RestController
@Validated
public class IngestionController {

    private final IngestionService ingestionService;
    private final DocumentRecordRepository repository;
    private final AuthenticatedUser authenticatedUser;

    public IngestionController(IngestionService ingestionService,
                               DocumentRecordRepository repository,
                               AuthenticatedUser authenticatedUser) {
        this.ingestionService = ingestionService;
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping(value = "/api/documents", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentView upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("allowedGroup") @NotBlank String allowedGroup,
            Authentication authentication) {

        requireGroupAccess(authentication, allowedGroup);
        DocumentRecord record = ingestionService.ingest(
                file, allowedGroup, authenticatedUser.username(authentication));
        return DocumentView.from(record);
    }

    @GetMapping("/api/documents")
    public List<DocumentView> list(Authentication authentication) {
        List<DocumentRecord> records = authenticatedUser.isAdmin(authentication)
                ? repository.findAllByOrderByUploadedAtDesc()
                : repository.findByAllowedGroupIn(authenticatedUser.documentGroups(authentication));
        return records.stream()
                .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                .map(DocumentView::from)
                .toList();
    }

    @DeleteMapping("/api/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        DocumentRecord record = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such document"));
        requireGroupAccess(authentication, record.getAllowedGroup());
        ingestionService.delete(record);
    }

    private void requireGroupAccess(Authentication authentication, String group) {
        if (authenticatedUser.isAdmin(authentication)) {
            return;
        }
        Set<String> groups = authenticatedUser.documentGroups(authentication);
        if (!groups.contains(group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a member of group '" + group + "'");
        }
    }

    public record DocumentView(String id, String filename, String allowedGroup,
                               String uploadedBy, LocalDateTime uploadedAt, int chunkCount) {
        static DocumentView from(DocumentRecord r) {
            return new DocumentView(r.getId().toString(), r.getFilename(), r.getAllowedGroup(),
                    r.getUploadedBy(), r.getUploadedAt(), r.getChunkCount());
        }
    }
}

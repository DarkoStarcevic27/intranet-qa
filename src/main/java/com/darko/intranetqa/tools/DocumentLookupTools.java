package com.darko.intranetqa.tools;

import com.darko.intranetqa.ingestion.DocumentRecord;
import com.darko.intranetqa.ingestion.DocumentRecordRepository;
import com.darko.intranetqa.security.AuthenticatedUser;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tools the model can call in addition to retrieval. These run inside the
 * same authenticated request as the chat call, so they read the caller's
 * groups off the security context and apply the same access rule as the
 * vector search does, rather than trusting whatever the model asks for.
 */
@Component
public class DocumentLookupTools {

    private final DocumentRecordRepository documents;
    private final AuthenticatedUser authenticatedUser;

    public DocumentLookupTools(DocumentRecordRepository documents, AuthenticatedUser authenticatedUser) {
        this.documents = documents;
        this.authenticatedUser = authenticatedUser;
    }

    @Tool(description = "List the documents currently indexed and visible to the current user, "
            + "with filename and last-updated date. Use this when the user asks what documents "
            + "are available rather than asking a question the documents should answer.")
    public String listAvailableDocuments() {
        var groups = authenticatedUser.documentGroups(SecurityContextHolder.getContext().getAuthentication());
        List<DocumentRecord> visible = documents.findByAllowedGroupIn(groups);

        if (visible.isEmpty()) {
            return "No documents are currently visible to this user.";
        }

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        StringBuilder sb = new StringBuilder();
        for (DocumentRecord doc : visible) {
            sb.append("- ").append(doc.getFilename())
              .append(" (group: ").append(doc.getAllowedGroup())
              .append(", indexed ").append(doc.getUploadedAt().format(fmt))
              .append(")\n");
        }
        return sb.toString();
    }
}

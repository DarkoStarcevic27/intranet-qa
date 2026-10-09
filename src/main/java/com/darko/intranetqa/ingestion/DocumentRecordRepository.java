package com.darko.intranetqa.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, java.util.UUID> {

    List<DocumentRecord> findByAllowedGroupIn(Collection<String> allowedGroups);

    List<DocumentRecord> findAllByOrderByUploadedAtDesc();

    boolean existsByFilenameAndAllowedGroup(String filename, String allowedGroup);
}

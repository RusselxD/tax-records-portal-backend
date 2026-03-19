package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    void deleteAllByClientId(UUID clientId);
}

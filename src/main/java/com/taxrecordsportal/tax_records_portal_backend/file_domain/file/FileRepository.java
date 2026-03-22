package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    @Modifying
    @Query("DELETE FROM FileEntity f WHERE f.client.id = :clientId")
    void deleteAllByClientId(@Param("clientId") UUID clientId);

    @EntityGraph(attributePaths = {"client", "client.createdBy", "client.accountants", "client.user"})
    Optional<FileEntity> findWithClientById(UUID id);
}

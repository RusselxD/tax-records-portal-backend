package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientInfoSnapshotRepository extends JpaRepository<ClientInfoSnapshot, UUID> {

    Optional<ClientInfoSnapshot> findTopByClientIdOrderByCreatedAtDesc(UUID clientId);
}

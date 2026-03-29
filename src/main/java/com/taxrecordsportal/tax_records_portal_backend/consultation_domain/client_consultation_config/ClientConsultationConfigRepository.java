package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientConsultationConfigRepository extends JpaRepository<ClientConsultationConfig, UUID> {

    Optional<ClientConsultationConfig> findByClientId(UUID clientId);
}

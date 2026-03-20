package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientInfoTemplateRepository extends JpaRepository<ClientInfoTemplate, Integer> {
    Optional<ClientInfoTemplate> findFirstBy();
}

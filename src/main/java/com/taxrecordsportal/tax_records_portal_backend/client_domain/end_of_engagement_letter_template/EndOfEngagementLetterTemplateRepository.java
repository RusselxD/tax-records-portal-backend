package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EndOfEngagementLetterTemplateRepository extends JpaRepository<EndOfEngagementLetterTemplate, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}

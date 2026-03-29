package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EndOfEngagementLetterTemplateListItemResponse(
        UUID id,
        String name,
        String createdBy,
        Instant createdAt
) {}

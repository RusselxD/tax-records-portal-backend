package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EndOfEngagementLetterTemplateResponse(
        UUID id,
        String name,
        Map<String, Object> body,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}

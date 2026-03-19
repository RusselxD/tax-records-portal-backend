package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileUpdateReviewResponse(
        UUID clientId,
        String clientName,
        ClientInfoTaskStatus status,
        Submitter submittedBy,
        Instant submittedAt,
        String comment,
        List<ChangedSection> changes
) {

    public record Submitter(UUID id, String name) {}

    public record ChangedSection(String sectionKey, JsonNode current, JsonNode submitted) {}
}

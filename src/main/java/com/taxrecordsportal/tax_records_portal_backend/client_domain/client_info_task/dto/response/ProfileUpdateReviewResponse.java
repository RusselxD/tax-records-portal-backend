package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileUpdateReviewResponse(
        UUID clientId,
        String clientName,
        ClientInfoTaskStatus status,
        Submitter submittedBy,
        Instant submittedAt,
        String comment,
        List<SectionDiff> sections
) {

    public record Submitter(UUID id, String name) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SectionDiff(
            String sectionKey,
            String sectionLabel,
            List<ChangeEntry> changes
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChangeEntry(
            String type,
            String field,
            @JsonProperty("old") String oldValue,
            @JsonProperty("new") String newValue,
            String value,
            String itemLabel,
            List<FieldDiff> fields
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldDiff(
            String field,
            @JsonProperty("old") String oldValue,
            @JsonProperty("new") String newValue
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldValue(
            String field,
            String value
    ) {}
}

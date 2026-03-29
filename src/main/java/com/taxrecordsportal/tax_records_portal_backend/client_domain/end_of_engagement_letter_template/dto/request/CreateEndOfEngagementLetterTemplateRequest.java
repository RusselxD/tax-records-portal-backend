package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateEndOfEngagementLetterTemplateRequest(
        @NotBlank String name,
        @NotNull Map<String, Object> body
) {}

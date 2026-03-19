package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddWorkingLinkRequest(
        @NotBlank String url,
        @NotBlank String label
) {}

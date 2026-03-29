package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddWorkingLinkRequest(
        @NotBlank
        @Size(max = 2000)
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        String url,

        @NotBlank
        @Size(max = 500)
        String label
) {}

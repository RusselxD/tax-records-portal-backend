package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTaxTaskNameRequest(
        @NotBlank String name
) {
}

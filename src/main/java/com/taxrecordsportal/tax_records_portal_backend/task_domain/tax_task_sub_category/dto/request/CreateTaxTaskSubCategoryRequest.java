package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTaxTaskSubCategoryRequest(
        @NotBlank String name
) {
}

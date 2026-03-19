package com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmployeePositionCreateRequest(
        @NotBlank(message = "Position name is required.")
        String name
) {
}

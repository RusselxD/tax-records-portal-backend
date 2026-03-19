package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BulkTaskRowRequest(
        @NotNull(message = "Client is required.") UUID clientId,
        @NotBlank(message = "Category is required.") String category,
        @NotBlank(message = "Sub category is required.") String subCategory,
        @NotBlank(message = "Task name is required.") String taskName,
        @NotNull(message = "Year is required.") Integer year,
        @NotBlank(message = "Period is required.") String period,
        @NotBlank(message = "Deadline is required.") String deadline,
        String description,
        @NotNull(message = "Assigned to is required.") UUID assignedToId
) {}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaxRecordTaskRequest(
        @NotNull UUID clientId,
        @NotNull(message = "Category is required.") Integer categoryId,
        @NotNull(message = "Sub category is required.") Integer subCategoryId,
        @NotNull(message = "Task name is required.") Integer taskNameId,
        @NotNull(message = "Year is required.") @Min(1900) Integer year,
        @NotNull Period period,
        @NotNull LocalDate deadline,
        @Size(max = 2000) String description,
        @NotNull UUID assignedToId
) {}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaxRecordTaskProgressListItemResponse(
        UUID id,
        String clientName,
        String categoryName,
        String subCategoryName,
        String taskName,
        int year,
        Period period,
        LocalDate deadline,
        String createdBy,
        Instant createdAt
) {
}

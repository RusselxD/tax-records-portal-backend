package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaxRecordTaskRejectedItemResponse(
        UUID id,
        String clientName,
        String taskName,
        String categoryName,
        String subCategoryName,
        Period period,
        int year,
        LocalDate deadline,
        boolean isOverdue,
        String createdBy,
        Instant createdAt
) {}

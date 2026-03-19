package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaxRecordTaskOverdueItemResponse(
        UUID id,
        String clientName,
        String taskName,
        String categoryName,
        String subCategoryName,
        Period period,
        int year,
        TaxRecordTaskStatus status,
        LocalDate deadline,
        String createdBy,
        Instant createdAt
) {}

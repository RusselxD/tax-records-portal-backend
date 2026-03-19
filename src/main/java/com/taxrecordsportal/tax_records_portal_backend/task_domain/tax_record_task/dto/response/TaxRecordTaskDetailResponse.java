package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaxRecordTaskDetailResponse(
        UUID id,
        UUID clientId,
        String clientDisplayName,
        String categoryName,
        String subCategoryName,
        String taskName,
        int year,
        Period period,
        String description,
        Instant deadline,
        TaxRecordTaskStatus status,
        List<String> assignedTo,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaxRecordTaskListItemResponse(
        UUID id,
        String clientDisplayName,
        String categoryName,
        String subCategoryName,
        String taskName,
        int year,
        Period period,
        TaxRecordTaskStatus status,
        Instant deadline,
        List<String> assignedTo,
        boolean isOverdue,
        String createdBy,
        Instant createdAt
) {}

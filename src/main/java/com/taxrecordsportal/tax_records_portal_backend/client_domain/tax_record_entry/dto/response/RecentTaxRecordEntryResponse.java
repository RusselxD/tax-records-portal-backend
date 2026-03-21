package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;

import java.time.Instant;
import java.util.UUID;

public record RecentTaxRecordEntryResponse(
        UUID id,
        int categoryId,
        String categoryName,
        int subCategoryId,
        String subCategoryName,
        int taskNameId,
        String taskName,
        int year,
        Period period,
        Instant createdAt
) {}

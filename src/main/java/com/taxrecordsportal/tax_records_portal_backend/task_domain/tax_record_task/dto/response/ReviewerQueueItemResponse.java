package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReviewerQueueItemResponse(
        UUID id,
        String clientName,
        String taskName,
        String categoryName,
        String subCategoryName,
        int year,
        Period period,
        LocalDate deadline,
        boolean isOverdue,
        List<String> assignedTo,
        Instant submittedAt
) {}

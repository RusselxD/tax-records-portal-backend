package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientTaxRecordTaskItem(
        UUID id,
        String taskName,
        String categoryName,
        Period period,
        int year,
        TaxRecordTaskStatus status,
        LocalDate deadline,
        boolean isOverdue,
        List<String> assignedTo
) {}

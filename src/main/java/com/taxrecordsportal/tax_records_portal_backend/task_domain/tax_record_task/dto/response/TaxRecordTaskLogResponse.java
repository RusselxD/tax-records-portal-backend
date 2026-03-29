package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log.TaxRecordTaskLogAction;

import java.time.Instant;
import java.util.UUID;

public record TaxRecordTaskLogResponse(
        UUID id,
        TaxRecordTaskLogAction action,
        boolean hasComment,
        String performedBy,
        Instant performedAt
) {}

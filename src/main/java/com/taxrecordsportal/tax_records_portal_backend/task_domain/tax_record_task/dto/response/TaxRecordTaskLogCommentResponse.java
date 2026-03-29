package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import java.util.Map;
import java.util.UUID;

public record TaxRecordTaskLogCommentResponse(
        UUID id,
        Map<String, Object> comment
) {}

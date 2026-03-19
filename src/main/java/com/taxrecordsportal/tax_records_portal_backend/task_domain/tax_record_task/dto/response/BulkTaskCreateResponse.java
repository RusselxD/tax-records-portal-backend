package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import java.util.List;

public record BulkTaskCreateResponse(
        int created,
        int failed,
        List<BulkTaskError> errors
) {
    public record BulkTaskError(int index, String message) {}
}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto;

import java.util.UUID;

public record WorkingFileItem(
        String type,
        UUID fileId,
        String fileName,
        String url,
        String label
) {
}

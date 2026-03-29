package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;

import java.util.List;
import java.util.UUID;

public record TaxRecordTaskFilesResponse(
        List<WorkingFileItem> workingFiles,
        FileItem outputFile,
        FileItem proofOfFilingFile
) {
    public record FileItem(UUID id, String name) {}
}

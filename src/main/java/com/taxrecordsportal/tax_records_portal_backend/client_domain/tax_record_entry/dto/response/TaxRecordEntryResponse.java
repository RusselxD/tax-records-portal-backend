package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;

import java.util.List;
import java.util.UUID;

public record TaxRecordEntryResponse(
        UUID id,
        String categoryName,
        String subCategoryName,
        String taskName,
        int year,
        Period period,
        List<WorkingFileItem> workingFiles,
        FileRef outputFile,
        FileRef proofOfFilingFile
) {
    public record FileRef(UUID id, String name) {}
    public record WorkingFileItem(String type, UUID fileId, String fileName, String url, String label) {}
}

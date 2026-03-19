package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;

import java.util.List;
import java.util.UUID;

public record TaxRecordEntryResponse(
        UUID id,
        String category,
        String subCategory,
        String taskName,
        int year,
        Period period,
        List<WorkingFileItem> workingFiles,
        UUID outputFileId,
        String outputFileName,
        UUID proofOfFilingFileId,
        String proofOfFilingFileName
) {
}

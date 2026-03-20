package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response;

public record DrillDownItemResponse(
        String id,
        String label,
        long count
) {
}

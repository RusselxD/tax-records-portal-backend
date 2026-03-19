package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record BirComplianceItem(
        String category,
        String taxReturnName,
        String deadline,
        boolean applicable,
        String notes
) {
}

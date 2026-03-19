package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant;

public record CategoryCountItem(
        String category,
        int total,
        int active,
        int completed
) {}

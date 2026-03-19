package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant;

public record MonthlyThroughputItem(
        String month,
        int completed
) {}

package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto;

public record MonthlyThroughputItem(
        String month,
        int completed
) {}

package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.system;

public record TasksByCategorySystemItem(
        String category,
        int open,
        int submitted,
        int rejected,
        int approvedForFiling,
        int filed,
        int completed
) {}

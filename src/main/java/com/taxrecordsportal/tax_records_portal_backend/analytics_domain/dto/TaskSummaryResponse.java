package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto;

public record TaskSummaryResponse(
        int open,
        int submitted,
        int rejected,
        int approvedForFiling,
        int filed,
        int completed,
        int overdue,
        int completedThisMonth
) {}

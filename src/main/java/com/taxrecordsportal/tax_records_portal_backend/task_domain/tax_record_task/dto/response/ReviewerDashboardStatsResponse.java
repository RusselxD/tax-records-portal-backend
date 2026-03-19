package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

public record ReviewerDashboardStatsResponse(
        int awaitingReview,
        int newToday,
        int approvedToday,
        Double approvalRateThisMonth
) {}

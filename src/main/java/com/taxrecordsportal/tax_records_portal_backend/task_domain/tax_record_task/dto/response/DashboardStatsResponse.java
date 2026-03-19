package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

public record DashboardStatsResponse(
        long openTasks,
        long newToday,
        long submittedTasks,
        long forFilingTasks
) {}

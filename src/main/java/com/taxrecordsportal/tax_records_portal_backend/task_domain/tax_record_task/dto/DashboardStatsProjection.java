package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto;

public interface DashboardStatsProjection {
    long getOpenTasks();
    long getNewToday();
    long getSubmittedTasks();
    long getForFilingTasks();
}

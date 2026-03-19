package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto;

public interface ReviewerTaskStatsProjection {
    long getAwaitingReview();
    long getNewToday();
}

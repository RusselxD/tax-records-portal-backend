package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface TaskSummaryProjection {
    long getOpen();
    long getSubmitted();
    long getRejected();
    long getApprovedForFiling();
    long getFiled();
    long getCompleted();
    long getOverdue();
}

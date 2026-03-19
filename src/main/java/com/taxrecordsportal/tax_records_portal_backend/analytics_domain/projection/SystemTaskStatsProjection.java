package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface SystemTaskStatsProjection {
    long getTotal();
    long getOpen();
    long getSubmitted();
    long getApprovedForFiling();
    long getFiled();
    long getCompleted();
    long getRejected();
    long getOverdue();
    long getDueToday();
    long getDueThisWeek();
    long getCreatedThisMonth();
}

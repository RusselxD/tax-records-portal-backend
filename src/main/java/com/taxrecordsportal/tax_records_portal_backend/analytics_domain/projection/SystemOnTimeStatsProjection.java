package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface SystemOnTimeStatsProjection {
    long getTotalCompleted();
    long getCompletedOnTime();
    long getCompletedLate();
    Double getAvgCompletionDays();
}

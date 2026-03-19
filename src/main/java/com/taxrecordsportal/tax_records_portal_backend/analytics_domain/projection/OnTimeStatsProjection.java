package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface OnTimeStatsProjection {
    long getTotalCompleted();
    long getCompletedOnTime();
    long getCompletedLate();
}

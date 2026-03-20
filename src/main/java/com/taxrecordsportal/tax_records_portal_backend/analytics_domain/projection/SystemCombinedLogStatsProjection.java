package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface SystemCombinedLogStatsProjection {
    // From SystemLogStatsProjection
    long getCompletedThisMonth();
    long getRejectedThisMonth();
    // From SystemOnTimeStatsProjection
    long getTotalCompleted();
    long getCompletedOnTime();
    long getCompletedLate();
    Double getAvgCompletionDays();
    // From QualityStatsProjection
    long getTotalSubmitted();
    long getFirstAttemptApproved();
    Double getAvgRejectionCycles();
}

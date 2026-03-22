package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface AccountantLogStatsProjection {
    // Card 3 — Productivity
    long getSubmittedThisMonth();
    long getCompletedThisMonth();

    // Card 4 — Efficiency
    long getTotalCompleted();
    long getCompletedOnTime();
    Double getAvgCompletionDays();

    // Card 5 — Quality
    long getTotalApproved();
    long getFirstAttemptApproved();
    Double getAvgRejectionCycles();

    // Card 6 — Responsiveness
    Double getAvgDaysToFirstSubmit();
    Double getAvgRejectionTurnaroundDays();

    // Card 8 — Trend
    long getCompletedLastMonth();
}

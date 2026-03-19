package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface QualityStatsProjection {
    long getTotalSubmitted();
    long getFirstAttemptApproved();
    Double getAvgRejectionCycles();
}

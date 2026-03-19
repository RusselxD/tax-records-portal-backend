package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto;

public record QualityMetricsResponse(
        int totalSubmitted,
        int firstAttemptApproved,
        double firstAttemptApprovalRate,
        double avgRejectionCyclesPerTask
) {}

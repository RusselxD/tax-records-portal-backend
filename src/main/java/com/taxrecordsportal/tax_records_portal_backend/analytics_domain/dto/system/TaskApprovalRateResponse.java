package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.system;

public record TaskApprovalRateResponse(
        int approvedRate,
        int rejectedRate
) {}

package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface ApprovalRateProjection {
    long getApproved();
    long getRejected();
}

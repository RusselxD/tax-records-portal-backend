package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface SystemProfileStatsProjection {
    long getTotal();
    long getOnboarding();
    long getUpdates();
}

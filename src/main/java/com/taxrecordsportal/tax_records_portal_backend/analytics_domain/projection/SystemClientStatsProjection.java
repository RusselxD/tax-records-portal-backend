package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface SystemClientStatsProjection {
    long getTotal();
    long getOnboarding();
    long getActive();
    long getOffboarding();
    long getInactive();
}
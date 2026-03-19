package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface CategoryStatsProjection {
    String getCategory();
    long getTotal();
    long getActive();
    long getCompleted();
}

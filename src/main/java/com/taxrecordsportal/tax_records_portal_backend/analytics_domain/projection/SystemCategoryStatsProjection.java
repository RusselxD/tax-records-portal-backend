package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface SystemCategoryStatsProjection {
    String getCategory();
    long getOpen();
    long getSubmitted();
    long getRejected();
    long getApprovedForFiling();
    long getFiled();
    long getCompleted();
}

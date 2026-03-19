package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface ClientStatusCountProjection {
    String getStatus();
    long getCount();
}

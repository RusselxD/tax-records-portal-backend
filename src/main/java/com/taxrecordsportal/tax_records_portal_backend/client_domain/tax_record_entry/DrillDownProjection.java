package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

public interface DrillDownProjection {
    String getId();
    String getLabel();
    long getCount();
}

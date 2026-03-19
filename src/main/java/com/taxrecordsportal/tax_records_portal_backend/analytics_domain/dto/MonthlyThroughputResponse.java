package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto;

import java.util.List;

public record MonthlyThroughputResponse(
        List<MonthlyThroughputItem> data
) {}

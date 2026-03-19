package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.util.List;

public record BirComplianceBreakdown(
        List<BirComplianceItem> items,
        String othersSpecify
) {
}

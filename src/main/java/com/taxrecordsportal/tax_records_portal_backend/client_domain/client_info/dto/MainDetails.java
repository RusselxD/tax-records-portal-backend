package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.EngagementStatus;

public record MainDetails(
        String mreCode,
        DateField commencementOfWork,
        EngagementStatus engagementStatus
) {
}

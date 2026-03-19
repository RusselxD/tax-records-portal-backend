package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.EngagementStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.DateField;

import java.util.List;
import java.util.UUID;

public record MainDetailsPatchRequest(
        String mreCode,
        DateField commencementOfWork,
        EngagementStatus engagementStatus,
        List<UUID> csdOosAccountantIds,
        UUID qtdAccountantId
) {
}

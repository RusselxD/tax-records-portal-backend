package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskType;

import java.time.Instant;
import java.util.UUID;

public record ProfileReviewListItemResponse(
        UUID id,
        UUID clientId,
        String clientName,
        ClientInfoTaskType type,
        ClientInfoTaskStatus status,
        String submittedBy,
        Instant submittedAt
) {
}

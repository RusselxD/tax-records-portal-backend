package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.ClientInfoTaskLogAction;

import java.time.Instant;
import java.util.UUID;

public record ClientInfoTaskLogResponse(
        UUID id,
        String performedBy,
        ClientInfoTaskLogAction action,
        boolean hasComment,
        Instant createdAt
) {
}

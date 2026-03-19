package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.ClientInfoTaskLogAction;

import java.time.Instant;

public record ClientInfoLogItemResponse(
        String performedBy,
        ClientInfoTaskLogAction action,
        String comment,
        Instant createdAt
) {
}

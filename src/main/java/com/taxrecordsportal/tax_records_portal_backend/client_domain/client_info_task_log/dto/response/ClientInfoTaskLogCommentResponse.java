package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.dto.response;

import java.util.Map;
import java.util.UUID;

public record ClientInfoTaskLogCommentResponse(
        UUID id,
        Map<String, Object> comment
) {
}

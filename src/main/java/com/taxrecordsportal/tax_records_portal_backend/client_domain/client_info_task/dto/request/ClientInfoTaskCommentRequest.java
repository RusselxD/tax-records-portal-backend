package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request;

import java.util.Map;

public record ClientInfoTaskCommentRequest(
        Map<String, Object> comment
) {
}

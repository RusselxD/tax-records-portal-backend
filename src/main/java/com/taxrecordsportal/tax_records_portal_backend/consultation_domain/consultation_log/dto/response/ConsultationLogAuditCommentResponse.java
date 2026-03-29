package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response;

import java.util.Map;
import java.util.UUID;

public record ConsultationLogAuditCommentResponse(
        UUID id,
        Map<String, Object> comment
) {}

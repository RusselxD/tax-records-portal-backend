package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request;

import java.util.Map;

public record ConsultationLogActionRequest(
        Map<String, Object> comment
) {}

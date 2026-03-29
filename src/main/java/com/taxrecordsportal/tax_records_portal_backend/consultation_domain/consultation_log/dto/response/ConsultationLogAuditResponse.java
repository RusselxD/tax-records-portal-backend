package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit.ConsultationLogAuditAction;

import java.time.Instant;
import java.util.UUID;

public record ConsultationLogAuditResponse(
        UUID id,
        ConsultationLogAuditAction action,
        boolean hasComment,
        String performedByName,
        Instant createdAt
) {}

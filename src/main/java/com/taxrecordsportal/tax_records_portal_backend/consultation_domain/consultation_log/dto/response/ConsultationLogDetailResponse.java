package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationBillableType;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationLogStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConsultationLogDetailResponse(
        UUID id,
        UUID clientId,
        String clientDisplayName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal hours,
        String platform,
        String subject,
        Map<String, Object> notes,
        List<FileItemResponse> attachments,
        ConsultationBillableType billableType,
        ConsultationLogStatus status,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {
    public record FileItemResponse(UUID id, String name) {}
}

package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationBillableType;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationLogStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ConsultationLogListItemResponse(
        UUID id,
        UUID clientId,
        String clientDisplayName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal hours,
        String platform,
        String subject,
        ConsultationBillableType billableType,
        ConsultationLogStatus status,
        String createdByName,
        Instant createdAt
) {}

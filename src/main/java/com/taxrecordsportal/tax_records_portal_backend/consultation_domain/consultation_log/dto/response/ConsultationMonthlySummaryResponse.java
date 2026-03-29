package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response;

import java.math.BigDecimal;

public record ConsultationMonthlySummaryResponse(
        int year,
        int month,
        BigDecimal totalHoursConsumed,
        BigDecimal courtesyHours,
        BigDecimal billableHours,
        BigDecimal includedHours,
        BigDecimal remainingIncluded,
        BigDecimal excessHours,
        BigDecimal excessRate,
        BigDecimal estimatedExcessFee
) {}

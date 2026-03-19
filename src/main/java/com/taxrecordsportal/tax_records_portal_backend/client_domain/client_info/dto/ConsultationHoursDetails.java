package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConsultationHoursDetails(
        BigDecimal freeHoursPerMonth,
        BigDecimal ratePerHourAfterFree,
        List<ConsultationEntry> consultations,
        BigDecimal totalBillableAmount
) {
}

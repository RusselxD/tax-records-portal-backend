package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ConsultationEntry(
        DateField date,
        String timeStarted,
        String timeEnded,
        Map<String, Object> topicsAndDocumentation,
        BigDecimal numberOfHours,
        String platform,
        int amount,
        BigDecimal vat
) {
}

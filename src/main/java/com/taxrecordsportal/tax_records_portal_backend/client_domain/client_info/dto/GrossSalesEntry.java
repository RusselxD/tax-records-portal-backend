package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.math.BigDecimal;

public record GrossSalesEntry(
        int year,
        BigDecimal amount
) {
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.time.LocalDate;

public record DateField(
        LocalDate date,
        boolean isImportant
) {
}

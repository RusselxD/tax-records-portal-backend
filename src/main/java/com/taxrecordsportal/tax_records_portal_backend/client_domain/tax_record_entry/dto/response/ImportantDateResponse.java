package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response;

import java.time.LocalDate;

public record ImportantDateResponse(
        LocalDate date,
        String label
) {}

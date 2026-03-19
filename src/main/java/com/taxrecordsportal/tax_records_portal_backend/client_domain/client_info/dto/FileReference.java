package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.util.UUID;

public record FileReference(
        UUID id,
        String name
) {
}

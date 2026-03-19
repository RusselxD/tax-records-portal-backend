package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import jakarta.validation.constraints.NotNull;

public record ClientStatusUpdateRequest(
        @NotNull(message = "Status is required.")
        ClientStatus status
) {}

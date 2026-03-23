package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;

import java.time.Instant;
import java.util.UUID;

public record OnboardingClientListItemResponse(
        UUID id,
        String name,
        String email,
        ClientStatus status,
        Instant createdAt,
        Instant updatedAt,
        boolean hasActiveTask,
        UUID activeTaskId,
        UUID lastTaskId,
        boolean handedOff
) {
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClientOffboardingListItemResponse(
        UUID id,
        String name,
        String email,
        ClientStatus status,
        String offboardingAccountantName,
        LocalDate endOfEngagementDate,
        LocalDate deactivationDate,
        boolean taxRecordsProtected,
        boolean endOfEngagementLetterSent,
        Instant createdAt,
        Instant updatedAt
) {}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ClientOffboardRequest(
        @NotNull UUID oosAccountantId,
        @NotNull LocalDate endOfEngagementDate,
        LocalDate deactivationDate
) {}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ReassignClientAccountantsRequest(
        @NotEmpty @Size(max = 20) List<UUID> csdOosAccountantIds,
        @NotNull UUID qtdAccountantId
) {}

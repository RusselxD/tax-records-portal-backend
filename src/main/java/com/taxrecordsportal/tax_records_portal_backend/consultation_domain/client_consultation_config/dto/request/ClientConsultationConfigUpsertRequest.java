package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ClientConsultationConfigUpsertRequest(
        @NotNull @DecimalMin("0") BigDecimal includedHours,
        @NotNull @DecimalMin("0") BigDecimal excessRate
) {}

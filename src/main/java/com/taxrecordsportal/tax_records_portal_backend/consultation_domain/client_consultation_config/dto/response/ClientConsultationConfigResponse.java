package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientConsultationConfigResponse(
        UUID clientId,
        BigDecimal includedHours,
        BigDecimal excessRate
) {}

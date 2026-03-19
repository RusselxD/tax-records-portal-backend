package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;

import java.util.List;
import java.util.UUID;

public record ClientSummaryResponse(
        UUID id,
        String name,
        ClientStatus status,
        String mreCode,
        String taxpayerClassification,
        List<String> assignedCsdOosAccountants,
        List<String> assignedQtdAccountants
) {}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnboardingDetails(
        String nameOfGroupChat,
        String platformUsed,
        String gcCreatedBy,
        DateField gcCreatedDate,
        List<PendingActionItem> pendingActionItems
) {
}

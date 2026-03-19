package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.util.List;

public record OnboardingDetails(
        String nameOfGroupChat,
        String platformUsed,
        String gcCreatedBy,
        DateField gcCreatedDate,
        List<OnboardingMeetingEntry> meetings,
        List<PendingActionItem> pendingActionItems
) {
}

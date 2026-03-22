package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant;

import java.util.UUID;

public record AccountantOverviewItemResponse(
        UUID id,
        String name,
        String position,
        String roleKey,
        String profileUrl,
        int activeTasks,
        int assignedClients,
        int overdueTasks
) {}

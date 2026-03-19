package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;

import java.util.UUID;

public record ClientPortfolioItem(
        UUID clientId,
        String clientName,
        ClientStatus status,
        long totalTasks,
        long pendingTasks,
        long overdueTasks,
        String nearestDeadline
) {}

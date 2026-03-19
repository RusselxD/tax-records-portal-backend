package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;

import java.util.List;
import java.util.UUID;

public record ClientListItemResponse(
        UUID id,
        String clientName,
        List<String> accountants,
        long totalTasks,
        long pendingTasks,
        long overdueTasks,
        String nearestDeadline,
        ClientStatus status
) {}

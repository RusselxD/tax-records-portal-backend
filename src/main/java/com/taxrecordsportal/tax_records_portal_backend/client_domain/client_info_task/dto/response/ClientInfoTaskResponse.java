package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;

import java.util.List;
import java.util.UUID;

public record ClientInfoTaskResponse(
        UUID clientId,
        String clientDisplayName,
        String taxpayerClassification,
        ClientStatus clientStatus,
        boolean hasActiveTask,
        UUID activeTaskId,
        ClientInfoTaskType activeTaskType,
        ClientInfoTaskStatus lastReviewStatus,
        List<AccountantListItemResponse> assignedCsdOosAccountants,
        List<AccountantListItemResponse> assignedQtdAccountants,
        String pocEmail
) {}

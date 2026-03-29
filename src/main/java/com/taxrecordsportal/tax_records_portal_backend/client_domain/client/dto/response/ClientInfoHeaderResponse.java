package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientInfoHeaderResponse(
        String displayName,
        String taxpayerClassification,
        ClientStatus status,
        String pocEmail,
        boolean isProfileApproved,
        boolean handedOff,
        AccountantsInfo accountants,
        TaskReviewInfo taskReview,
        OffboardingInfo offboarding
) {
    public record AccountantsInfo(
            List<AccountantListItemResponse> csdOos,
            List<AccountantListItemResponse> qtd
    ) {}

    public record TaskReviewInfo(
            boolean hasActiveTask,
            UUID activeTaskId,
            ClientInfoTaskType activeTaskType,
            ClientInfoTaskStatus lastReviewStatus
    ) {}

    public record OffboardingInfo(
            String accountantName,
            LocalDate endOfEngagementDate,
            LocalDate deactivationDate,
            boolean taxRecordsProtected,
            boolean endOfEngagementLetterSent
    ) {}
}

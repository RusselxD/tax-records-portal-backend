package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;

import java.util.List;

public record ClientInfoResponse(
        // Header (computed, read-only)
        String clientDisplayName,
        String taxpayerClassification,
        ClientStatus clientStatus,
        List<AccountantListItemResponse> assignedCsdOosAccountants,
        List<AccountantListItemResponse> assignedQtdAccountants,
        boolean hasActiveTask,
        ClientInfoTaskStatus lastReviewStatus,

        // 7 JSONB sections
        MainDetails mainDetails,
        ClientInformation clientInformation,
        CorporateOfficerInformation corporateOfficerInformation,
        List<AccessCredentialDetails> accessCredentials,
        ScopeOfEngagementDetails scopeOfEngagement,
        List<ProfessionalFeeEntry> professionalFees,
        OnboardingDetails onboardingDetails
) {
}

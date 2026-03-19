package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;

import java.time.Instant;
import java.util.List;

public record ArchiveSnapshotResponse(
        // Header (computed from snapshot)
        String clientDisplayName,
        String taxpayerClassification,
        List<AccountantListItemResponse> assignedCsdOosAccountants,
        List<AccountantListItemResponse> assignedQtdAccountants,
        Instant submittedAt,

        // 7 JSONB sections (frozen)
        MainDetails mainDetails,
        ClientInformation clientInformation,
        CorporateOfficerInformation corporateOfficerInformation,
        List<AccessCredentialDetails> accessCredentials,
        ScopeOfEngagementDetails scopeOfEngagement,
        List<ProfessionalFeeEntry> professionalFees,
        OnboardingDetails onboardingDetails
) {
}

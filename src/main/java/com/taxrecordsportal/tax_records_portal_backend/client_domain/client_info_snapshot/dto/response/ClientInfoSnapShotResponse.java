package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;

import java.time.Instant;
import java.util.List;

public record ClientInfoSnapShotResponse(
        MainDetails mainDetails,
        ClientInformation clientInformation,
        CorporateOfficerInformation corporateOfficerInformation,
        List<AccessCredentialDetails> accessCredentials,
        ScopeOfEngagementDetails scopeOfEngagement,
        List<ProfessionalFeeEntry> professionalFees,
        OnboardingDetails onboardingDetails,
        Instant createdAt
) {
}

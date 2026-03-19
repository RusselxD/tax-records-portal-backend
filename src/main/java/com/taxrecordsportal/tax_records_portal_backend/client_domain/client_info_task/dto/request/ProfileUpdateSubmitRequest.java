package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;

import java.util.List;

public record ProfileUpdateSubmitRequest(
        String comment,
        MainDetails mainDetails,
        ClientInformation clientInformation,
        CorporateOfficerInformation corporateOfficerInformation,
        List<AccessCredentialDetails> accessCredentials,
        ScopeOfEngagementDetails scopeOfEngagement,
        List<ProfessionalFeeEntry> professionalFees,
        OnboardingDetails onboardingDetails
) {}

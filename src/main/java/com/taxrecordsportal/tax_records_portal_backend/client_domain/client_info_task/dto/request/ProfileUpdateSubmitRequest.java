package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;

import java.util.List;
import java.util.Map;

public record ProfileUpdateSubmitRequest(
        Map<String, Object> comment,
        MainDetails mainDetails,
        ClientInformation clientInformation,
        CorporateOfficerInformation corporateOfficerInformation,
        List<AccessCredentialDetails> accessCredentials,
        ScopeOfEngagementDetails scopeOfEngagement,
        List<ProfessionalFeeEntry> professionalFees,
        OnboardingDetails onboardingDetails
) {}

package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant;

public record OnboardingPipelineResponse(
        int onboarding,
        int activeClient,
        int offboarding,
        int inactiveClient,
        int total
) {}

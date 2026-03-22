package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.diff;

import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum SectionMetadata {

    MAIN_DETAILS("mainDetails", "Main Details"),
    CLIENT_INFORMATION("clientInformation", "Client Information"),
    CORPORATE_OFFICER_INFORMATION("corporateOfficerInformation", "Owner's / Corporate Officer's Information"),
    ACCESS_CREDENTIALS("accessCredentials", "Access Credentials"),
    SCOPE_OF_ENGAGEMENT("scopeOfEngagement", "Scope of Engagement"),
    PROFESSIONAL_FEES("professionalFees", "Professional Fees"),
    ONBOARDING_DETAILS("onboardingDetails", "Onboarding Details");

    private final String key;
    private final String label;

    private static final Map<String, SectionMetadata> BY_KEY = Stream.of(values())
            .collect(Collectors.toMap(SectionMetadata::getKey, Function.identity()));

    SectionMetadata(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static SectionMetadata fromKey(String key) {
        return BY_KEY.get(key);
    }
}

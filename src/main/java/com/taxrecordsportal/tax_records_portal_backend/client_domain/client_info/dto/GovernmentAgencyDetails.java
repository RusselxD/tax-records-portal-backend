package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record GovernmentAgencyDetails(
        DateField dateOfRegistration,
        String registrationNumber,
        FileReference certificatesAndDocuments,
        String others
) {
}

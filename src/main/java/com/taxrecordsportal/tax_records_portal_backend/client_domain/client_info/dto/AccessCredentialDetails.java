package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record AccessCredentialDetails(
        String platform,
        LinkReference linkToPlatform,
        String usernameOrEmail,
        String password,
        String notes
) {
}

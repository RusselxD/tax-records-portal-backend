package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;

import java.util.UUID;

public record ClientAccountResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String profileUrl,
        UserStatus status
) {
}

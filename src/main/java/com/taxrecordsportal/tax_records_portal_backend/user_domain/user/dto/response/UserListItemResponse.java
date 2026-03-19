package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;

import java.util.UUID;

public record UserListItemResponse(
        UUID id,
        String name,
        String email,
        String roleName,
        String position,
        String profileUrl,
        UserStatus status
) {
}

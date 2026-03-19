package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;

import java.util.UUID;

public record AccountantListItemResponse(
        UUID id,
        String displayName,
        String position,
        String role,
        RoleKey roleKey
) {
}

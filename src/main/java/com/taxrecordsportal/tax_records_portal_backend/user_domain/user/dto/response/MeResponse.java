package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;

import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String name,
        String role,
        RoleKey roleKey,
        String position,
        UserStatus status,
        String profileUrl,
        List<String> permissions,
        List<UserTitle> titles
) {}

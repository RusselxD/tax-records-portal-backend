package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;

import java.util.List;
import java.util.UUID;

public record UserListItemResponse(
        UUID id,
        String firstName,
        String lastName,
        String name,
        String email,
        String roleName,
        Integer roleId,
        String position,
        Integer positionId,
        String profileUrl,
        UserStatus status,
        List<UserTitle> titles
) {}

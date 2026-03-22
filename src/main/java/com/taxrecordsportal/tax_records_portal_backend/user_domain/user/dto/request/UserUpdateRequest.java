package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;
import jakarta.validation.constraints.Email;

import java.util.List;

public record UserUpdateRequest(
        String firstName,
        String lastName,

        @Email(message = "Invalid email.")
        String email,

        Integer roleId,
        Integer positionId,
        List<UserTitle> titles
) {}

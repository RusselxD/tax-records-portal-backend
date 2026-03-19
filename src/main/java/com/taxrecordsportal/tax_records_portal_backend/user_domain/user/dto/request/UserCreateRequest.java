package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserCreateRequest(

        @NotBlank(message = "First name is required.")
        String firstName,

        @NotBlank(message = "Last name is required.")
        String lastName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        String email,

        @NotNull(message = "Role ID is required.")
        Integer roleId,

        @NotNull(message = "Position ID is required.")
        Integer positionId,

        List<UserTitle> titles
) {
}

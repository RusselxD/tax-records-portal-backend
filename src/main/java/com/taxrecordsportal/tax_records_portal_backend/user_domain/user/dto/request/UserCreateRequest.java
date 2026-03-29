package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(

        @NotBlank(message = "First name is required.")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        @Size(max = 255)
        String email,

        @NotNull(message = "Role ID is required.")
        Integer roleId,

        @NotNull(message = "Position ID is required.")
        Integer positionId,

        @Size(max = 10)
        List<UserTitle> titles
) {
}

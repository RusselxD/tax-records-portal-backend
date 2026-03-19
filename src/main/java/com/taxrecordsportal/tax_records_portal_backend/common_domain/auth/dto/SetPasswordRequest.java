package com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(

        @NotBlank(message = "Token is required.")
        String token,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        String password
) {
}

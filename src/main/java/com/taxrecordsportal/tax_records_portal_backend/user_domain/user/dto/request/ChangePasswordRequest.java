package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        String newPassword
) {}

package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        String newPassword
) {}

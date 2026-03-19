package com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "Refresh token is required.")
        String refreshToken
){}

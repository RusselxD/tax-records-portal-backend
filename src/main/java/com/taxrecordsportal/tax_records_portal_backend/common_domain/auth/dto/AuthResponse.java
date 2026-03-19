package com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
){}
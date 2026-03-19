package com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.dto;

public record ActivateAccountResponse (
    Boolean valid,
    String email
) {}

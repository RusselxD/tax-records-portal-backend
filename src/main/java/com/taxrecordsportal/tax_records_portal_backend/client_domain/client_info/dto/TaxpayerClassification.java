package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaxpayerClassification {
    MICRO("Micro Taxpayer"),
    SMALL("Small Taxpayer"),
    MEDIUM("Medium Taxpayer"),
    LARGE("Large Taxpayer");

    private final String displayName;
}

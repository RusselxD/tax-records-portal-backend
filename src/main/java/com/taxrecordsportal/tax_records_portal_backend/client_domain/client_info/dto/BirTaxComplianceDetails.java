package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.util.List;

public record BirTaxComplianceDetails(
        List<GrossSalesEntry> grossSales,
        TaxpayerClassification taxpayerClassification,
        boolean topWithholding,
        DateField dateClassifiedTopWithholding,
        String incomeTaxRegime
) {
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record CorporateOfficerDetails(
        String name,
        DateField birthday,
        String address,
        String position,
        FileReference idScannedWith3Signature
) {
}

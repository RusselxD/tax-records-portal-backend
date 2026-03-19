package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record DtiDetails(
        // DTI Registration
        String dtiRegistrationNo,
        DateField dtiDateOfRegistration,
        DateField dtiDateOfExpiration,
        FileReference dtiBusinessRegistrationCertificate,
        FileReference dtiBnrsUndertakingForm,
        FileReference dtiOfficialReceipt,

        // BMBE Compliance
        String bmbeTotalAssets,
        String bmbeNo,
        DateField bmbeDateOfRegistration,
        DateField bmbeDateOfExpiration,
        FileReference bmbeOfficialReceipt,

        // Others
        String others
) {
}

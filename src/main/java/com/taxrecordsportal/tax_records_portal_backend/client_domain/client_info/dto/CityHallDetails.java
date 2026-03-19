package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record CityHallDetails(
        String businessPermitCity,
        String businessPermitNumber,
        DateField dateOfRegistration,
        String renewalBasis,
        DateField quarterlyDeadlineQ2,
        DateField quarterlyDeadlineQ3,
        DateField quarterlyDeadlineQ4,
        DateField permitExpirationDate,
        PermitDetails firePermit,
        PermitDetails sanitaryPermit,
        PermitDetails otherPermit,
        FileReference mayorBusinessPermit,
        FileReference businessPermitPlate,
        FileReference billingAssessment,
        FileReference officialReceiptOfPayment,
        FileReference sanitaryPermitFile,
        FileReference firePermitFile,
        FileReference barangayPermit,
        FileReference communityTaxCertificate,
        FileReference locationalClearance,
        FileReference environmentalClearance,
        FileReference comprehensiveGeneralLiabilityInsurance
) {
}

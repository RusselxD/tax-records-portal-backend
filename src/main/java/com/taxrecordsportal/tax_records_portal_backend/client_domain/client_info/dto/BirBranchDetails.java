package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record BirBranchDetails(
        String businessTradeName,
        String tin,
        String rdo,
        String completeRegisteredAddress,
        String birRegistrationNumber,
        String typeOfBusiness,
        String classification,
        DateField dateOfBirRegistration,
        FileReference birCertificateOfRegistration,
        FileReference birForm1901,
        FileReference birForm1921Atp,
        FileReference birForm1905,
        FileReference sampleInvoiceReceipts,
        FileReference niriPoster,
        FileReference birBookOfAccountsStamp,
        FileReference birForm2000Dst,
        FileReference contractOfLease
) {
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record SecDetails(
        DateField dateOfIncorporation,
        String secRegistrationNumber,
        DateField dateOfActualMeetingPerBylaws,
        String primaryPurposePerArticles,
        String corporationCategory,
        FileReference secCertificateOfIncorporation,
        FileReference articlesOfIncorporation,
        FileReference bylawsOfCorporation,
        FileReference certificateOfAuthentication,
        FileReference authorizeFilerSecretaryCertificate,
        FileReference secOfficialReceipts,
        FileReference latestGisOrAppointmentOfOfficer,
        FileReference stockAndTransferBook,
        FileReference boardResolutionsSecretaryCertificate,
        FileReference previousYearAfsAndItr,
        String others
) {
}

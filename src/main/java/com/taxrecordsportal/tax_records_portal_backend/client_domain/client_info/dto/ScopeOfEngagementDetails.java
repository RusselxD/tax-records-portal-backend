package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScopeOfEngagementDetails(
        // Header
        DateField dateOfEngagementLetter,
        List<FileReference> engagementLetters,

        // A. Documents & Information Gathering
        Map<String, Object> salesInvoicesAndDocuments,
        Map<String, Object> purchaseAndExpenseDocuments,
        Map<String, Object> payrollDocuments,
        Map<String, Object> sssPhilhealthHdmfDocuments,
        Map<String, Object> businessPermitsLicensesAndOtherDocuments,
        Map<String, Object> additionalNotes,

        // B. Client Engagements
        Map<String, Object> taxCompliance,
        BookOfAccounts bookOfAccounts,
        String bookkeepingPermitNo,
        List<FileReference> looseleafCertificateAndBirTemplates,
        List<RegisteredBookEntry> registeredBooks,
        Map<String, Object> bookkeepingProcess,
        Map<String, Object> sssPhilhealthHdmfEngagement,
        Map<String, Object> paymentAssistance,
        String consultationFreeAllowance,
        String consultationExcessRate,

        // C. Required Deliverable & Report
        Map<String, Object> standardDeliverable,
        String requiredDeliverableOthers
) {
}

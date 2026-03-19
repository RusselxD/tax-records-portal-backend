package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.OrganizationType;

import java.util.List;

public record ClientInformation(
        // Header fields
        String registeredName,
        String tradeName,
        int numberOfBranches,
        OrganizationType organizationType,

        // Sub-sections
        BirBranchDetails birMainBranch,
        List<BirBranchDetails> birBranches,
        BirTaxComplianceDetails birTaxCompliance,
        BirComplianceBreakdown birComplianceBreakdown,
        DtiDetails dtiDetails,
        SecDetails secDetails,
        GovernmentAgencyDetails sssDetails,
        GovernmentAgencyDetails philhealthDetails,
        GovernmentAgencyDetails hdmfDetails,
        List<CityHallDetails> cityHallDetails
) {
}

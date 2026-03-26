package com.taxrecordsportal.tax_records_portal_backend.client_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template.ClientInfoTemplate;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template.ClientInfoTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(3)
public class ClientInfoTemplateSeeder implements CommandLineRunner {

    private final ClientInfoTemplateRepository clientInfoTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (clientInfoTemplateRepository.count() > 0) {
            return;
        }

        ClientInfoTemplate template = new ClientInfoTemplate();

        template.setMainDetails(new MainDetails(
                null,                   // mreCode
                emptyDateField()        // commencementOfWork
        ));

        template.setClientInformation(new ClientInformation(
                null,                   // registeredName
                null,                   // tradeName
                0,                      // numberOfBranches
                null,                   // organizationType
                emptyBirBranchDetails(),        // birMainBranch (always present, head office)
                List.of(),                      // birBranches (additional branches)
                new BirTaxComplianceDetails(
                        List.of(),          // grossSales
                        null,               // taxpayerClassification (computed)
                        false,              // topWithholding
                        emptyDateField(),   // dateClassifiedTopWithholding
                        null                // incomeTaxRegime
                ),
                new BirComplianceBreakdown(birComplianceItems(), null),
                new DtiDetails(
                        null, emptyDateField(), emptyDateField(), // DTI: no, dateOfReg, dateOfExp
                        null, null, null,                         // DTI files: cert, bnrs, receipt
                        null, null, emptyDateField(), emptyDateField(), // BMBE: assets, no, dateOfReg, dateOfExp
                        null,                                     // BMBE file: receipt
                        null                                      // others
                ),
                new SecDetails(
                        emptyDateField(), null, emptyDateField(), null, null, // dateOfIncorp, secRegNo, meetingDate, purpose, category
                        null, null, null, null, null,                         // files: cert, articles, bylaws, auth, filer
                        null, null, null, null, null,                         // files: receipts, gis, stock, resolutions, afs
                        null                                                  // others
                ),
                emptyGovernmentAgencyDetails(), // sssDetails
                emptyGovernmentAgencyDetails(), // philhealthDetails
                emptyGovernmentAgencyDetails(), // hdmfDetails
                List.of(emptyCityHallDetails())
        ));

        template.setCorporateOfficerInformation(new CorporateOfficerInformation(
                List.of(emptyCorporateOfficerDetails()),
                new PointOfContactDetails(
                        null, null, null, null, null, null, null
                )
        ));

        template.setAccessCredentials(List.of(emptyAccessCredentialDetails()));

        template.setScopeOfEngagement(new ScopeOfEngagementDetails(
                emptyDateField(), // dateOfEngagementLetter
                List.of(),        // engagementLetters
                // A. Documents & Information Gathering
                null, null, null, null, null, null,
                // B. Client Engagements
                null, null, null, List.of(), List.of(), null, null, null, null, null,
                // C. Required Deliverable & Report
                null, null
        ));

        template.setProfessionalFees(List.of(
                new ProfessionalFeeEntry("Monthly Professional Fees", null),
                new ProfessionalFeeEntry("General Information Sheet (GIS)", null),
                new ProfessionalFeeEntry("Annual Alphalist of Employees & BIR Form 2316", null),
                new ProfessionalFeeEntry("Inventory List Submission", null),
                new ProfessionalFeeEntry("Sworn Statement of Gross Remittance - Online Seller", null),
                new ProfessionalFeeEntry("Sworn Statement - Professional", null),
                new ProfessionalFeeEntry("Lessee Information Sheet (LIS)", null),
                new ProfessionalFeeEntry("Loose-leaf", null),
                new ProfessionalFeeEntry("Annual Escalation Fees", null)
        ));

        template.setOnboardingDetails(new OnboardingDetails(
                null,             // nameOfGroupChat
                null,             // platformUsed
                null,             // gcCreatedBy
                emptyDateField(), // gcCreatedDate
                List.of()         // pendingActionItems
        ));

        clientInfoTemplateRepository.save(template);
    }

    private DateField emptyDateField() {
        return new DateField(null, false);
    }

    private BirBranchDetails emptyBirBranchDetails() {
        return new BirBranchDetails(
                null, null, null, null,
                null, null, null, emptyDateField(),
                null, null, null, null,
                null, null, null, null, null
        );
    }

    private GovernmentAgencyDetails emptyGovernmentAgencyDetails() {
        return new GovernmentAgencyDetails(
                emptyDateField(), null, null, null
        );
    }

    private CorporateOfficerDetails emptyCorporateOfficerDetails() {
        return new CorporateOfficerDetails(
                null, emptyDateField(), null, null, null
        );
    }

    private AccessCredentialDetails emptyAccessCredentialDetails() {
        return new AccessCredentialDetails(null, null, null, null, null);
    }

    private CityHallDetails emptyCityHallDetails() {
        return new CityHallDetails(
                null, null, emptyDateField(),
                null,
                emptyDateField(), emptyDateField(), emptyDateField(),
                emptyDateField(),
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null
        );
    }

    private List<BirComplianceItem> birComplianceItems() {
        return List.of(
                // Income Tax Return (ITR)
                new BirComplianceItem("Income Tax Return (ITR)", "(BIR Form 1701) Annual Income Tax Return For Individuals", "April 15 of the following year", false, null),
                new BirComplianceItem("Income Tax Return (ITR)", "(BIR Form 1701Q) Quarterly Income Tax Return For Individuals", "May 15, August 15, November 15", false, null),
                new BirComplianceItem("Income Tax Return (ITR)", "(BIR Form 1702) Annual Income Tax Return For Corporation", "April 15 of the following year", false, null),
                new BirComplianceItem("Income Tax Return (ITR)", "(BIR Form 1702Q) Quarterly Income Tax Return For Corporation", "May 15, August 15, November 15", false, null),
                new BirComplianceItem("Income Tax Return (ITR)", "(SAWT) Summary Alphalist of Withholding Tax", "May 15, August 15, November 15, April 15 of the Following Year", false, null),

                // Value Added Tax (VAT)
                new BirComplianceItem("Value Added Tax (VAT)", "(BIR Form 2550Q) Quarterly Value-Added Tax (VAT) Return", "25 days after the close of each taxable quarter", false, null),
                new BirComplianceItem("Value Added Tax (VAT)", "(SLSPI) Summary List of Sales, Purchases and Importation", "25 days after the close of each taxable quarter", false, null),

                // Percentage Tax (PT)
                new BirComplianceItem("Percentage Tax (PT)", "(BIR Form 2551Q) Quarterly Percentage Tax Return", "25 days after the close of each taxable quarter", false, null),

                // Withholding Tax Compensation
                new BirComplianceItem("Withholding Tax Compensation", "(BIR Form 1601-C) Monthly Remittance Return of Income Taxes Withheld on Compensation", "10th day of the following month", false, null),
                new BirComplianceItem("Withholding Tax Compensation", "(BIR Form 1604C) Annual Information Return on Compensation Income", "January 31 of the Following Year", false, null),

                // Withholding Tax Expanded
                new BirComplianceItem("Withholding Tax Expanded", "(BIR Form 0619-E) Monthly Remittance Form of Creditable Income Taxes Withheld (Expanded)", "10th day of the following month", false, null),
                new BirComplianceItem("Withholding Tax Expanded", "(BIR Form 1601EQ) Quarterly Remittance Return", "Last day of the month following the quarter", false, null),
                new BirComplianceItem("Withholding Tax Expanded", "(QAP - E) Quarterly Alphalist of Payees", "Last day of the month following the quarter", false, null),
                new BirComplianceItem("Withholding Tax Expanded", "(BIR Form 1604E) Annual Information Return of Creditable Income Taxes Withheld (Expanded)", "January 31 of the Following Year", false, null),
                new BirComplianceItem("Withholding Tax Expanded", "(Alpha - Payees (E)) Annual Alphalist of Payees - Expanded", "January 31 of the Following Year", false, null),

                // Withholding Tax Final
                new BirComplianceItem("Withholding Tax Final", "(BIR Form 0619-F) Monthly Remittance Form of Final Income Taxes Withheld", "10th day of the following month", false, null),
                new BirComplianceItem("Withholding Tax Final", "(BIR Form 1601FQ) Quarterly Remittance Return", "Last day of the month following the quarter", false, null),
                new BirComplianceItem("Withholding Tax Final", "(QAP - F) Quarterly Alphalist of Payees", "Last day of the month following the quarter", false, null),
                new BirComplianceItem("Withholding Tax Final", "(BIR Form 1601FQ) Quarterly Remittance Return", "January 31 of the Following Year", false, null),
                new BirComplianceItem("Withholding Tax Final", "(QAP - F) Quarterly Alphalist of Payees", "January 31 of the Following Year", false, null),

                // Withholding Tax - VAT Remittance
                new BirComplianceItem("Withholding Tax - VAT Remittance", "(1600-VT) Monthly Remittance Return of Value-Added Tax Withheld", "10th day of the following month", false, null),

                // e-Sales Report Submission
                new BirComplianceItem("e-Sales Report Submission", "Monthly eSales Reporting", "8th of the Following Month", false, null),

                // Other BIR Filings
                new BirComplianceItem("Other BIR Filings", "Annual Alphalist of Employees & BIR Form 2316", "January 31 of the Following Year", false, null),
                new BirComplianceItem("Other BIR Filings", "Inventory List Submission", "January 30 of the Following Year", false, null),
                new BirComplianceItem("Other BIR Filings", "Sworn Statement of Gross Remittance - Online Seller", "January 20 of the Following Year", false, null),
                new BirComplianceItem("Other BIR Filings", "Sworn Statement - Professional", "January 15 of the Following Year", false, null),
                new BirComplianceItem("Other BIR Filings", "Lessee Information Sheet (LIS)", "July 30 of the current Year and January 31 of the following year", false, null),
                new BirComplianceItem("Other BIR Filings", "Preparation of BIR Form 2307 & BIR Form 2304", "When Needed", false, null),
                new BirComplianceItem("Other BIR Filings", "(BIR Form 2000) Documentary Stamp Tax", "5th day following the end of the month when transaction occurred", false, null)
        );
    }
}

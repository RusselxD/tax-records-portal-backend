package com.taxrecordsportal.tax_records_portal_backend.task_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(6)
public class TaxTaskNameSeeder implements CommandLineRunner {

    private final TaxTaskSubCategoryRepository subCategoryRepository;
    private final TaxTaskNameRepository taskNameRepository;

    // Key: "CategoryName::SubCategoryName" → List of task names
    private static final Map<String, List<String>> TASK_NAMES_BY_SUB_CATEGORY = buildTaskNames();

    private static Map<String, List<String>> buildTaskNames() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        // ── SEC Files ──
        map.put("SEC Files::AFS", List.of("Audited Financial Statements"));
        map.put("SEC Files::UFS", List.of("Unaudited Financial Statements"));
        map.put("SEC Files::GIS", List.of("General Information Sheet"));
        map.put("SEC Files::GFFS", List.of("General Form Financial Statements"));
        map.put("SEC Files::Articles & By Laws", List.of("Articles of Incorporation & By Laws"));

        // ── DTI Files ──
        map.put("DTI Files::DTI Certificate", List.of("DTI Documents"));

        // ── City Hall Files ──
        map.put("City Hall Files::Business Permit", List.of("Business Permits Documents"));
        map.put("City Hall Files::Fire Permit", List.of("Fire Permit Documents"));
        map.put("City Hall Files::Sanitary Permit", List.of("Sanitary Permit Documents"));
        map.put("City Hall Files::Community Tax Certificate", List.of("Community Tax Certificate Documents"));

        // ── Client Documents & Records ──
        map.put("Client Documents & Records::Sales", List.of("Sales Invoices, BIR Form 2307, and Other Sales Documents"));
        map.put("Client Documents & Records::Purchases & Expenses", List.of("Purchase and Expense Supporting Documents"));
        map.put("Client Documents & Records::Payroll", List.of("Payroll Documents"));
        map.put("Client Documents & Records::SSS, PH & HDMF", List.of("SSS, PhilHealth, and HDMF Documents"));
        map.put("Client Documents & Records::Business Permits & Others", List.of("Business Permits, Licenses, and Other Documents"));

        // ── Book of Accounts ──
        map.put("Book of Accounts::CRJ", List.of("Cash Receipts Journal"));
        map.put("Book of Accounts::CDJ", List.of("Cash Disbursements Journal"));
        map.put("Book of Accounts::GJ", List.of("General Journal"));
        map.put("Book of Accounts::GL", List.of("General Ledger"));
        map.put("Book of Accounts::SSJ", List.of("Subsidiary Sales Journal"));
        map.put("Book of Accounts::SPJ", List.of("Subsidiary Purchase Journal"));

        // ── SSS, Philhealth, & HDMF Files ──
        map.put("SSS, Philhealth, & HDMF Files::SSS Contributions", List.of("Payment Reference Number (PRN) - Contribution & Proof of Payment"));
        map.put("SSS, Philhealth, & HDMF Files::SSS Loans", List.of("Payment Reference Number (PRN) - Loan & Proof of Payment"));
        map.put("SSS, Philhealth, & HDMF Files::Philhealth Contribution", List.of("Statement of Premium Account (SPA) & Proof of Payment"));
        map.put("SSS, Philhealth, & HDMF Files::HDMF Contribution", List.of("Payment Instruction Form (PIF) - Contribution & Proof of Payment"));
        map.put("SSS, Philhealth, & HDMF Files::HDMF Loan", List.of("Payment Instruction Form (PIF) - Loan & Proof of Payment"));
        map.put("SSS, Philhealth, & HDMF Files::HDMF MP2", List.of("Payment Instruction Form (PIF) - MP2 & Proof of Payment"));

        // ── Internal Tax Compliance Reports ──
        map.put("Internal Tax Compliance Reports::Deliverables", List.of("Monthly Compliance Report"));

        // ── BIR Files ──
        map.put("BIR Files::Income Tax Return (ITR)", List.of(
                "(BIR Form 1701) Annual Income Tax Return For Individuals",
                "(BIR Form 1701Q) Quarterly Income Tax Return For Individuals",
                "(BIR Form 1702) Annual Income Tax Return For Corporation",
                "(BIR Form 1702Q) Quarterly Income Tax Return For Corporation",
                "(SAWT) Summary Alphalist of Withholding Tax"
        ));

        map.put("BIR Files::Value Added Tax (VAT)", List.of(
                "(BIR Form 2550Q) Quarterly Value-Added Tax (VAT) Return",
                "(SLSPI) Summary List of Sales, Purchases and Importation"
        ));

        map.put("BIR Files::Percentage Tax (PT)", List.of(
                "(BIR Form 2551Q) Quarterly Percentage Tax Return"
        ));

        map.put("BIR Files::Withholding Tax Compensation", List.of(
                "(BIR Form 1601-C) Monthly Remittance Return of Income Taxes Withheld on Compensation",
                "(BIR Form 1604C) Annual Information Return on Compensation Income"
        ));

        map.put("BIR Files::Withholding Tax Expanded", List.of(
                "(Alpha - Payees (E)) Annual Alphalist of Payees - Expanded",
                "(BIR Form 0619-E) Monthly Remittance Form of Creditable Income Taxes Withheld (Expanded)",
                "(BIR Form 1601EQ) Quarterly Remittance Return",
                "(BIR Form 1604E) Annual Information Return of Creditable Income Taxes Withheld (Expanded)",
                "(QAP - E) Quarterly Alphalist of Payees"
        ));

        map.put("BIR Files::Withholding Tax Final", List.of(
                "(BIR Form 0619-F) Monthly Remittance Form of Final Income Taxes Withheld",
                "(BIR Form 1601FQ) Quarterly Remittance Return",
                "(QAP - F) Quarterly Alphalist of Payees"
        ));

        map.put("BIR Files::Withholding Tax - VAT Remittance", List.of(
                "(1600-VT) Monthly Remittance Return of Value-Added Tax Withheld"
        ));

        map.put("BIR Files::e-Sales Report Submission", List.of(
                "Monthly eSales Reporting"
        ));

        map.put("BIR Files::Other BIR Filings", List.of(
                "Annual Alphalist of Employees & BIR Form 2316",
                "Inventory List Submission",
                "Lessee Information Sheet (LIS)",
                "Sworn Statement - Professional",
                "Sworn Statement of Gross Remittance - Online Seller"
        ));

        return map;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<TaxTaskSubCategory> subCategories = subCategoryRepository.findAll();

        for (TaxTaskSubCategory subCategory : subCategories) {
            String categoryName = subCategory.getCategory().getName();
            String key = categoryName + "::" + subCategory.getName();

            List<String> taskNames = TASK_NAMES_BY_SUB_CATEGORY.get(key);
            if (taskNames == null) continue;

            for (String name : taskNames) {
                if (!taskNameRepository.existsByNameAndSubCategoryId(name, subCategory.getId())) {
                    TaxTaskName taskName = new TaxTaskName();
                    taskName.setSubCategory(subCategory);
                    taskName.setName(name);
                    taskNameRepository.save(taskName);
                }
            }
        }
    }
}

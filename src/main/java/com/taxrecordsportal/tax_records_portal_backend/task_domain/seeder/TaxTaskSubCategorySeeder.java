package com.taxrecordsportal.tax_records_portal_backend.task_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(5)
public class TaxTaskSubCategorySeeder implements CommandLineRunner {

    private final TaxTaskCategoryRepository categoryRepository;
    private final TaxTaskSubCategoryRepository subCategoryRepository;

    private static final Map<String, List<String>> SUB_CATEGORIES_BY_CATEGORY = Map.of(
            "SEC Files", List.of(
                    "AFS",
                    "UFS",
                    "GIS",
                    "GFFS",
                    "Articles & By Laws"
            ),
            "City Hall Files", List.of(
                    "Business Permit",
                    "Fire Permit",
                    "Sanitary Permit",
                    "Community Tax Certificate"
            ),
            "DTI Files", List.of(
                    "DTI Certificate"
            ),
            "Book of Accounts", List.of(
                    "CRJ",
                    "CDJ",
                    "GJ",
                    "GL",
                    "SSJ",
                    "SPJ"
            ),
            "Internal Tax Compliance Reports", List.of(
                    "Deliverables",
                    "Others"
            ),
            "Client Documents & Records", List.of(
                    "Sales",
                    "Purchases & Expenses",
                    "Payroll",
                    "SSS, PH & HDMF",
                    "Business Permits & Others"
            ),
            "SSS, Philhealth, & HDMF Files", List.of(
                    "SSS Contributions",
                    "SSS Loans",
                    "Philhealth Contribution",
                    "HDMF Contribution",
                    "HDMF Loan",
                    "HDMF MP2"
            ),
            "BIR Files", List.of(
                    "Income Tax Return (ITR)",
                    "Value Added Tax (VAT)",
                    "Percentage Tax (PT)",
                    "Withholding Tax Compensation",
                    "Withholding Tax Expanded",
                    "Withholding Tax Final",
                    "Withholding Tax - VAT Remittance",
                    "e-Sales Report Submission",
                    "Other BIR Filings"
            ),
            "Adhoc & Consultation", List.of(
                    "Adhoc & Consultation"
            )
    );

    @Override
    @Transactional
    public void run(String... args) {
        List<TaxTaskCategory> categories = categoryRepository.findAll();

        for (TaxTaskCategory category : categories) {
            List<String> subCategoryNames = SUB_CATEGORIES_BY_CATEGORY.get(category.getName());
            if (subCategoryNames == null) continue;

            for (String name : subCategoryNames) {
                if (!subCategoryRepository.existsByNameAndCategoryId(name, category.getId())) {
                    TaxTaskSubCategory subCategory = new TaxTaskSubCategory();
                    subCategory.setCategory(category);
                    subCategory.setName(name);
                    subCategoryRepository.save(subCategory);
                }
            }
        }
    }
}

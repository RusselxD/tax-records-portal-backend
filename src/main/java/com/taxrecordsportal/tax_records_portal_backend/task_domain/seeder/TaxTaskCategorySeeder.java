package com.taxrecordsportal.tax_records_portal_backend.task_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(4)
public class TaxTaskCategorySeeder implements CommandLineRunner {

    private final TaxTaskCategoryRepository taxTaskCategoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<String> categories = List.of(
                "SEC Files",
                "City Hall Files",
                "DTI Files",
                "Book of Accounts",
                "Internal Tax Compliance Reports",
                "Client Documents & Records",
                "SSS, Philhealth, & HDMF Files",
                "BIR Files",
                "Adhoc & Consultation"
        );

        List<TaxTaskCategory> toSave = new java.util.ArrayList<>();
        for (String name : categories) {
            if (!taxTaskCategoryRepository.existsByName(name)) {
                TaxTaskCategory category = new TaxTaskCategory();
                category.setName(name);
                toSave.add(category);
            }
        }
        if (!toSave.isEmpty()) {
            taxTaskCategoryRepository.saveAll(toSave);
        }
    }
}

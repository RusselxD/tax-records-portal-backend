package com.taxrecordsportal.tax_records_portal_backend.billing_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.InvoiceTerm;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.InvoiceTermRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(7)
public class InvoiceTermSeeder implements CommandLineRunner {

    private final InvoiceTermRepository invoiceTermRepository;

    private static final Object[][] DEFAULTS = {
            {"Due on Receipt", 0},
            {"Net 3", 3},
            {"Net 5", 5},
            {"Net 7", 7},
            {"Net 15", 15},
    };

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        List<InvoiceTerm> toSave = new ArrayList<>();

        for (Object[] entry : DEFAULTS) {
            String name = (String) entry[0];
            int days = (int) entry[1];

            if (!invoiceTermRepository.existsByName(name)) {
                InvoiceTerm term = new InvoiceTerm();
                term.setName(name);
                term.setDays(days);
                toSave.add(term);
            }
        }

        if (!toSave.isEmpty()) {
            invoiceTermRepository.saveAll(toSave);
        }
    }
}

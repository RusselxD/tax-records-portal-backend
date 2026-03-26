package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceTermRepository extends JpaRepository<InvoiceTerm, Integer> {
    boolean existsByName(String name);
}

package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {
    List<InvoicePayment> findByInvoiceIdOrderByDateAsc(UUID invoiceId);
}

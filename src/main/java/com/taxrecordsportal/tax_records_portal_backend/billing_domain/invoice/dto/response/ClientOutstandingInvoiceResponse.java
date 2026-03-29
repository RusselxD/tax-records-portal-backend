package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClientOutstandingInvoiceResponse(
        UUID id,
        String invoiceNumber,
        LocalDate dueDate,
        BigDecimal amountDue,
        BigDecimal balance,
        InvoiceStatus status,
        boolean isOverdue
) {}

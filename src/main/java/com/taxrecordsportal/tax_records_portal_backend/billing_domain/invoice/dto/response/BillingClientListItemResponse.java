package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BillingClientListItemResponse(
        UUID clientId,
        String clientName,
        long totalInvoices,
        long unpaidInvoices,
        long partiallyPaidInvoices,
        long fullyPaidInvoices,
        BigDecimal totalAmountDue,
        BigDecimal totalBalance
) {}

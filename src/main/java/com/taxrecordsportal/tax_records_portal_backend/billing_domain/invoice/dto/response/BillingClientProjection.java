package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public interface BillingClientProjection {
    UUID getClientId();
    String getRegisteredName();
    String getTradeName();
    long getTotalInvoices();
    long getUnpaidInvoices();
    long getPartiallyPaidInvoices();
    long getFullyPaidInvoices();
    BigDecimal getTotalAmountDue();
    BigDecimal getTotalBalance();
}

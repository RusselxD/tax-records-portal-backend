package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceListItemResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String invoiceNumber,
        LocalDate invoiceDate,
        String termsName,
        LocalDate dueDate,
        String description,
        BigDecimal amountDue,
        BigDecimal balance,
        InvoiceStatus status,
        boolean emailSent,
        boolean hasEmailRecipients
) {}

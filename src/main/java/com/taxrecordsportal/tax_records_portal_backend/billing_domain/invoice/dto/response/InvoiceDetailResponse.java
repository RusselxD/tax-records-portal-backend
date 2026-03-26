package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.InvoiceStatus;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.response.InvoiceTermResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String invoiceNumber,
        LocalDate invoiceDate,
        InvoiceTermResponse terms,
        LocalDate dueDate,
        String description,
        BigDecimal amountDue,
        BigDecimal balance,
        InvoiceStatus status,
        List<FileItemResponse> attachments,
        List<InvoicePaymentResponse> payments,
        boolean emailSent,
        boolean hasEmailRecipients
) {}

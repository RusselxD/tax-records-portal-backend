package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoicePaymentResponse(
        UUID id,
        LocalDate date,
        BigDecimal amount,
        List<FileItemResponse> attachments,
        boolean emailSent
) {}

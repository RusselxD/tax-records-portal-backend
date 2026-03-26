package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReceivePaymentRequest(
        @NotNull(message = "Payment date is required.")
        LocalDate date,

        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        BigDecimal amount,

        List<FileReference> attachments
) {}

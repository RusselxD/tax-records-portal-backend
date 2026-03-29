package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceCreateRequest(
        @NotNull(message = "Client ID is required.")
        UUID clientId,

        @NotBlank(message = "Invoice number is required.")
        @Size(max = 100)
        String invoiceNumber,

        @NotNull(message = "Invoice date is required.")
        LocalDate invoiceDate,

        @NotNull(message = "Terms ID is required.")
        Integer termsId,

        @Size(max = 2000)
        String description,

        @NotNull(message = "Amount due is required.")
        @DecimalMin(value = "0.01", message = "Amount due must be greater than zero.")
        @Digits(integer = 12, fraction = 2, message = "Amount must have at most 12 integer digits and 2 decimal places.")
        BigDecimal amountDue,

        @Size(max = 20)
        List<FileReference> attachments
) {}

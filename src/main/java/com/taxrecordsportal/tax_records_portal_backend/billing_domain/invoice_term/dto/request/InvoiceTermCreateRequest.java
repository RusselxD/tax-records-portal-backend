package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceTermCreateRequest(
        @NotBlank(message = "Term name is required.")
        String name,

        @NotNull(message = "Days is required.")
        @Min(value = 0, message = "Days must be 0 or greater.")
        Integer days
) {}

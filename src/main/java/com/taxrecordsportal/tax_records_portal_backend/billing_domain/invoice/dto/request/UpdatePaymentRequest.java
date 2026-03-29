package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdatePaymentRequest(
        LocalDate date,

        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        @Digits(integer = 12, fraction = 2, message = "Amount must have at most 12 integer digits and 2 decimal places.")
        BigDecimal amount,

        List<FileReference> attachments
) {}

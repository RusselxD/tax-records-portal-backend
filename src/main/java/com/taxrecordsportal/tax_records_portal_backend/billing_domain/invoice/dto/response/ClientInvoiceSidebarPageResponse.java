package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ClientInvoiceSidebarPageResponse(
        List<ClientInvoiceSidebarItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        BigDecimal totalBalance
) {}

package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.request.InvoiceTermCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.response.InvoiceTermResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoice-terms")
@RequiredArgsConstructor
public class InvoiceTermController {

    private final InvoiceTermService invoiceTermService;

    @GetMapping
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<List<InvoiceTermResponse>> getAll() {
        return ResponseEntity.ok(invoiceTermService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<InvoiceTermResponse> create(
            @Valid @RequestBody InvoiceTermCreateRequest request
    ) {
        return ResponseEntity.ok(invoiceTermService.create(request));
    }
}

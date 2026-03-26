package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request.InvoiceCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request.ReceivePaymentRequest;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<PageResponse<BillingClientListItemResponse>> getBillingClients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invoiceService.getBillingClients(search, page, size));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<PageResponse<InvoiceListItemResponse>> getInvoices(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invoiceService.getInvoices(clientId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<InvoiceDetailResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @GetMapping("/me/outstanding")
    @PreAuthorize("hasAuthority('billing.view.own')")
    public ResponseEntity<List<ClientOutstandingInvoiceResponse>> getMyOutstandingInvoices() {
        return ResponseEntity.ok(invoiceService.getMyOutstandingInvoices());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('billing.view.own')")
    public ResponseEntity<PageResponse<ClientInvoiceListItemResponse>> getMyInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invoiceService.getMyInvoices(page, size));
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasAuthority('billing.view.own')")
    public ResponseEntity<InvoiceDetailResponse> getMyInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getMyInvoice(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<InvoiceDetailResponse> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request) {
        return ResponseEntity.ok(invoiceService.createInvoice(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<Void> sendInvoiceEmail(@PathVariable UUID id) {
        invoiceService.sendInvoiceEmail(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/void")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<Void> voidInvoice(@PathVariable UUID id) {
        invoiceService.voidInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<InvoicePaymentResponse> receivePayment(
            @PathVariable UUID id,
            @Valid @RequestBody ReceivePaymentRequest request) {
        return ResponseEntity.ok(invoiceService.receivePayment(id, request));
    }

    @PostMapping("/{id}/payments/{paymentId}/send-email")
    @PreAuthorize("hasAuthority('billing.manage')")
    public ResponseEntity<Void> sendPaymentEmail(@PathVariable UUID id, @PathVariable UUID paymentId) {
        invoiceService.sendPaymentEmail(id, paymentId);
        return ResponseEntity.noContent().build();
    }
}

package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.mapper;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.Invoice;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.InvoiceStatus;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_payment.InvoicePayment;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.response.InvoiceTermResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
public class InvoiceMapper {

    public InvoiceListItemResponse toListItem(Invoice invoice, String clientName, boolean hasEmailRecipients) {
        BigDecimal balance = computeBalance(invoice);
        return new InvoiceListItemResponse(
                invoice.getId(),
                invoice.getClient().getId(),
                clientName,
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getTerms().getName(),
                invoice.getDueDate(),
                invoice.getDescription(),
                invoice.getAmountDue(),
                balance,
                invoice.getStatus(),
                invoice.isEmailSent(),
                hasEmailRecipients
        );
    }

    public InvoiceDetailResponse toDetail(Invoice invoice, String clientName, boolean hasEmailRecipients) {
        BigDecimal balance = computeBalance(invoice);
        var terms = invoice.getTerms();
        var termsResponse = new InvoiceTermResponse(terms.getId(), terms.getName(), terms.getDays());

        List<InvoicePaymentResponse> payments = invoice.getPayments() != null
                ? invoice.getPayments().stream()
                    .map(this::toPaymentResponse)
                    .toList()
                : Collections.emptyList();

        List<FileItemResponse> attachments = invoice.getAttachments() != null
                ? invoice.getAttachments().stream()
                    .map(f -> new FileItemResponse(f.id(), f.name()))
                    .toList()
                : Collections.emptyList();

        return new InvoiceDetailResponse(
                invoice.getId(),
                invoice.getClient().getId(),
                clientName,
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                termsResponse,
                invoice.getDueDate(),
                invoice.getDescription(),
                invoice.getAmountDue(),
                balance,
                invoice.getStatus(),
                attachments,
                payments,
                invoice.isEmailSent(),
                hasEmailRecipients
        );
    }

    public InvoicePaymentResponse toPaymentResponse(InvoicePayment payment) {
        List<FileItemResponse> attachments = payment.getAttachments() != null
                ? payment.getAttachments().stream()
                    .map(f -> new FileItemResponse(f.id(), f.name()))
                    .toList()
                : Collections.emptyList();

        return new InvoicePaymentResponse(
                payment.getId(),
                payment.getDate(),
                payment.getAmount(),
                attachments,
                payment.isEmailSent()
        );
    }

    public ClientOutstandingInvoiceResponse toOutstandingItem(Invoice invoice) {
        BigDecimal balance = computeBalance(invoice);
        return new ClientOutstandingInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getDueDate(),
                invoice.getAmountDue(),
                balance,
                invoice.getStatus()
        );
    }

    public ClientInvoiceListItemResponse toClientListItem(Invoice invoice) {
        BigDecimal balance = computeBalance(invoice);
        boolean isOverdue = !invoice.isVoided()
                && (invoice.getStatus() == InvoiceStatus.UNPAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                && invoice.getDueDate() != null
                && !invoice.getDueDate().isAfter(LocalDate.now());
        return new ClientInvoiceListItemResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getDescription(),
                invoice.getAmountDue(),
                balance,
                invoice.getStatus(),
                isOverdue
        );
    }

    private BigDecimal computeBalance(Invoice invoice) {
        BigDecimal paid = sumPayments(invoice);
        return invoice.getAmountDue().subtract(paid);
    }

    public BigDecimal sumPayments(Invoice invoice) {
        if (invoice.getPayments() == null || invoice.getPayments().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return invoice.getPayments().stream()
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

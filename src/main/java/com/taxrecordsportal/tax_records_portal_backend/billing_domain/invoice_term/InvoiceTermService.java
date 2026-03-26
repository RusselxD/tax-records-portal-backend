package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.request.InvoiceTermCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.dto.response.InvoiceTermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceTermService {

    private final InvoiceTermRepository invoiceTermRepository;

    @Transactional(readOnly = true)
    public List<InvoiceTermResponse> getAll() {
        return invoiceTermRepository.findAll().stream()
                .map(t -> new InvoiceTermResponse(t.getId(), t.getName(), t.getDays()))
                .toList();
    }

    @Transactional
    public InvoiceTermResponse create(InvoiceTermCreateRequest request) {
        if (invoiceTermRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Term already exists.");
        }

        InvoiceTerm term = new InvoiceTerm();
        term.setName(request.name());
        term.setDays(request.days());
        InvoiceTerm saved = invoiceTermRepository.save(term);

        return new InvoiceTermResponse(saved.getId(), saved.getName(), saved.getDays());
    }
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.TaxRecordEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/tax-records")
@RequiredArgsConstructor
public class TaxRecordEntryController {

    private final TaxRecordEntryService taxRecordEntryService;

    @GetMapping
    @PreAuthorize("hasAuthority('tax_records.view.all') or hasAuthority('tax_records.view.own')")
    public ResponseEntity<List<TaxRecordEntryResponse>> getByClientId(@PathVariable UUID clientId) {
        return ResponseEntity.ok(taxRecordEntryService.getByClientId(clientId));
    }
}

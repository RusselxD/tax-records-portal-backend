package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.request.CreateEndOfEngagementLetterTemplateRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.request.UpdateEndOfEngagementLetterTemplateRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.response.EndOfEngagementLetterTemplateListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.response.EndOfEngagementLetterTemplateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/end-of-engagement-letter-templates")
@RequiredArgsConstructor
public class EndOfEngagementLetterTemplateController {

    private final EndOfEngagementLetterTemplateService service;

    @GetMapping
    @PreAuthorize("hasAuthority('client.create') or hasAuthority('client.manage')")
    public ResponseEntity<List<EndOfEngagementLetterTemplateListItemResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('client.create') or hasAuthority('client.manage')")
    public ResponseEntity<EndOfEngagementLetterTemplateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('client.create') or hasAuthority('client.manage')")
    public ResponseEntity<EndOfEngagementLetterTemplateResponse> create(
            @Valid @RequestBody CreateEndOfEngagementLetterTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client.create') or hasAuthority('client.manage')")
    public ResponseEntity<EndOfEngagementLetterTemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEndOfEngagementLetterTemplateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('client.create') or hasAuthority('client.manage')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

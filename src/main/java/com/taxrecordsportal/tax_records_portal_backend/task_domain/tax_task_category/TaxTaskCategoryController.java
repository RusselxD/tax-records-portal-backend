package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.CreateTaxRecordLookupRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordLookupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-record-categories")
@RequiredArgsConstructor
public class TaxTaskCategoryController {

    private final TaxTaskCategoryService taxTaskCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<TaxRecordLookupResponse>> getAll() {
        return ResponseEntity.ok(taxTaskCategoryService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<TaxRecordLookupResponse> create(@Valid @RequestBody CreateTaxRecordLookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxTaskCategoryService.create(request.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        taxTaskCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

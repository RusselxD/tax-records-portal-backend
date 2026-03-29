package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

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
@RequiredArgsConstructor
public class TaxTaskSubCategoryController {

    private final TaxTaskSubCategoryService taxTaskSubCategoryService;

    @GetMapping("/api/v1/tax-record-categories/{categoryId}/sub-categories")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<TaxRecordLookupResponse>> getByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(taxTaskSubCategoryService.getByCategory(categoryId));
    }

    @PostMapping("/api/v1/tax-record-categories/{categoryId}/sub-categories")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<TaxRecordLookupResponse> create(
            @PathVariable Integer categoryId,
            @Valid @RequestBody CreateTaxRecordLookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxTaskSubCategoryService.create(categoryId, request.name()));
    }

    @DeleteMapping("/api/v1/tax-record-categories/{categoryId}/sub-categories/{subCategoryId}")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<Void> delete(@PathVariable Integer categoryId, @PathVariable Integer subCategoryId) {
        taxTaskSubCategoryService.delete(categoryId, subCategoryId);
        return ResponseEntity.noContent().build();
    }
}

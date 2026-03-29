package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

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
public class TaxTaskNameController {

    private final TaxTaskNameService taxTaskNameService;

    @GetMapping("/api/v1/tax-record-sub-categories/{subCategoryId}/task-names")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<TaxRecordLookupResponse>> getBySubCategory(@PathVariable Integer subCategoryId) {
        return ResponseEntity.ok(taxTaskNameService.getBySubCategory(subCategoryId));
    }

    @PostMapping("/api/v1/tax-record-sub-categories/{subCategoryId}/task-names")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<TaxRecordLookupResponse> create(
            @PathVariable Integer subCategoryId,
            @Valid @RequestBody CreateTaxRecordLookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxTaskNameService.create(subCategoryId, request.name()));
    }

    @DeleteMapping("/api/v1/tax-record-sub-categories/{subCategoryId}/task-names/{taskNameId}")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<Void> delete(@PathVariable Integer subCategoryId, @PathVariable Integer taskNameId) {
        taxTaskNameService.delete(subCategoryId, taskNameId);
        return ResponseEntity.noContent().build();
    }
}

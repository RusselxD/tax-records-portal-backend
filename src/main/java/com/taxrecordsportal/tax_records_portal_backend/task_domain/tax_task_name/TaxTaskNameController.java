package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.dto.request.CreateTaxTaskNameRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.dto.response.TaxTaskNameResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-task-sub-categories/{subCategoryId}/task-names")
@RequiredArgsConstructor
public class TaxTaskNameController {

    private final TaxTaskNameService taxTaskNameService;

    @GetMapping
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<TaxTaskNameResponse>> getBySubCategory(@PathVariable Integer subCategoryId) {
        return ResponseEntity.ok(taxTaskNameService.getBySubCategory(subCategoryId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tax_task_category.manage')")
    public ResponseEntity<TaxTaskNameResponse> create(
            @PathVariable Integer subCategoryId,
            @Valid @RequestBody CreateTaxTaskNameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxTaskNameService.create(subCategoryId, request.name()));
    }
}

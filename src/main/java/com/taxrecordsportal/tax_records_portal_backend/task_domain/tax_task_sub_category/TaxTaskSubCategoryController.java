package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.dto.request.CreateTaxTaskSubCategoryRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.dto.response.TaxTaskSubCategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-task-categories/{categoryId}/sub-categories")
@RequiredArgsConstructor
public class TaxTaskSubCategoryController {

    private final TaxTaskSubCategoryService taxTaskSubCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<TaxTaskSubCategoryResponse>> getByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(taxTaskSubCategoryService.getByCategory(categoryId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tax_task_category.manage')")
    public ResponseEntity<TaxTaskSubCategoryResponse> create(
            @PathVariable Integer categoryId,
            @Valid @RequestBody CreateTaxTaskSubCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxTaskSubCategoryService.create(categoryId, request.name()));
    }
}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.dto.response.TaxTaskCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-task-categories")
@RequiredArgsConstructor
public class TaxTaskCategoryController {

    private final TaxTaskCategoryService taxTaskCategoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<TaxTaskCategoryResponse>> getAll() {
        return ResponseEntity.ok(taxTaskCategoryService.getAll());
    }
}

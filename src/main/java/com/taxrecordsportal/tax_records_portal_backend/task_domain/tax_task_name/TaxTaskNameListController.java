package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-task-names")
@RequiredArgsConstructor
public class TaxTaskNameListController {

    private final TaxTaskNameService taxTaskNameService;

    @GetMapping
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own') or hasAuthority('task.create')")
    public ResponseEntity<List<TaxRecordLookupResponse>> getAll() {
        return ResponseEntity.ok(taxTaskNameService.getAll());
    }
}

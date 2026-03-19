package com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.dto.request.EmployeePositionCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.dto.response.EmployeePositionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-positions")
@RequiredArgsConstructor
public class EmployeePositionController {

    private final EmployeePositionService employeePositionService;

    @GetMapping
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<List<EmployeePositionResponse>> getAll() {
        return ResponseEntity.ok(employeePositionService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<EmployeePositionResponse> create(
            @Valid @RequestBody EmployeePositionCreateRequest request
    ) {
        return ResponseEntity.ok(employeePositionService.create(request));
    }
}

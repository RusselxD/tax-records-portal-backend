package com.taxrecordsportal.tax_records_portal_backend.user_domain.role;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.dto.RoleListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/employees")
    public ResponseEntity<List<RoleListItemResponse>> getEmployeeRoles() {
        return ResponseEntity.ok(roleService.getEmployeeRoles());
    }
}

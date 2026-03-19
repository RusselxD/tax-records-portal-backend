package com.taxrecordsportal.tax_records_portal_backend.common_domain.auth;

import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.AuthResponse;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.LoginRequest;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.RefreshRequest;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.SetPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request){
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/set-password")
    public ResponseEntity<AuthResponse> setPassword(
            @Valid @RequestBody SetPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.setPassword(request));
    }
}

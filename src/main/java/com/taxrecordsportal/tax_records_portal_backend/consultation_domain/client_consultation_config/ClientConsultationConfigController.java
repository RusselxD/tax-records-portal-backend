package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config;

import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.dto.request.ClientConsultationConfigUpsertRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.dto.response.ClientConsultationConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client-consultation-configs")
@RequiredArgsConstructor
public class ClientConsultationConfigController {

    private final ClientConsultationConfigService configService;

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('consultation.view.own') or hasAuthority('consultation.view.all')")
    public ResponseEntity<ClientConsultationConfigResponse> getConfig(@PathVariable UUID clientId) {
        return ResponseEntity.ok(configService.getConfig(clientId));
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('consultation.config.manage')")
    public ResponseEntity<ClientConsultationConfigResponse> upsertConfig(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientConsultationConfigUpsertRequest request) {
        return ResponseEntity.ok(configService.upsertConfig(clientId, request));
    }
}

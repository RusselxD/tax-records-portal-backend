package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot.dto.response.ClientInfoSnapShotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client-info-snapshots")
@RequiredArgsConstructor
public class ClientInfoSnapshotController {

    private final ClientInfoSnapshotService clientInfoSnapshotService;

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client_info.view.own') or hasAuthority('client_info.view.all')")
    public ResponseEntity<ClientInfoSnapShotResponse> getClientInfoSnapshot(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientInfoSnapshotService.getClientInfoSnapshot(clientId));
    }
}

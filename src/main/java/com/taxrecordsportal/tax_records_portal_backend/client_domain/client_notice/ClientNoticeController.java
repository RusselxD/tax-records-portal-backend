package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.dto.request.CreateClientNoticeRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.dto.response.ClientNoticeResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientNoticeController {

    private final ClientNoticeService clientNoticeService;
    private final ClientRepository clientRepository;

    @PostMapping("/{clientId}/notices")
    @PreAuthorize("hasAuthority('reminder.create')")
    public ResponseEntity<ClientNoticeResponse> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateClientNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientNoticeService.create(clientId, request));
    }

    @GetMapping("/{clientId}/notices")
    @PreAuthorize("hasAuthority('reminder.create')")
    public ResponseEntity<List<ClientNoticeResponse>> getByClientId(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientNoticeService.getByClientId(clientId));
    }

    @DeleteMapping("/{clientId}/notices/{noticeId}")
    @PreAuthorize("hasAuthority('reminder.create')")
    public ResponseEntity<Void> delete(@PathVariable UUID clientId, @PathVariable Integer noticeId) {
        clientNoticeService.delete(clientId, noticeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/notices")
    public ResponseEntity<List<ClientNoticeResponse>> getMyNotices() {
        User currentUser = getCurrentUser();
        UUID clientId = clientRepository.findClientIdByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        return ResponseEntity.ok(clientNoticeService.getByClientId(clientId));
    }
}

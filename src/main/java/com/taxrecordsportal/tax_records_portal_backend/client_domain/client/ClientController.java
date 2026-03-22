package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.common.dto.ScrollResponse;
import tools.jackson.databind.JsonNode;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.ClientActivateRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.ClientStatusUpdateRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskService;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ArchiveSnapshotResponse;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final ClientInfoTaskService clientInfoTaskService;
    private final FileService fileService;

    @GetMapping
    @PreAuthorize("hasAuthority('client.view.all') or hasAuthority('client.view.own')")
    public ResponseEntity<PageResponse<ClientListItemResponse>> getClients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(clientService.getClients(search, page, size));
    }

    @GetMapping("/onboarding")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<List<OnboardingClientListItemResponse>> getOnboardingClients() {
        return ResponseEntity.ok(clientService.getOnboardingClients());
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<ScrollResponse<AssignedClientsListItemResponse>> getMyAssignedClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(clientService.getMyAssignedClients(page, size));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<ClientLookupResponse>> getActiveClients() {
        return ResponseEntity.ok(clientService.getActiveClients());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('client.create')")
    public ResponseEntity<ClientCreateResponse> createClient() {
        return ResponseEntity.ok(clientService.createClient());
    }

    @GetMapping("/info-template")
    @PreAuthorize("hasAuthority('client_info.create')")
    public ResponseEntity<ClientInfoResponse> getClientInfoTemplate() {
        return ResponseEntity.ok(clientService.getClientInfoTemplate());
    }

    @GetMapping("/me/info")
    public ResponseEntity<ClientInfoHeaderResponse> getMyClientInfoHeader() {
        return ResponseEntity.ok(clientService.getMyClientInfoHeader());
    }

    @GetMapping(value = "/me/info/{sectionKey}", produces = "application/json")
    public ResponseEntity<String> getMyClientInfoSection(@PathVariable String sectionKey) {
        return ResponseEntity.ok(clientService.getMyClientInfoSection(sectionKey));
    }

    @GetMapping("/me/engagement-letter-exists")
    public ResponseEntity<Map<String, Boolean>> hasEngagementLetter() {
        return ResponseEntity.ok(Map.of("exists", clientService.hasEngagementLetter()));
    }

    @GetMapping("/me/engagement-letter")
    public ResponseEntity<Resource> downloadEngagementLetter() {
        UUID fileId = clientService.getEngagementLetterFileId();
        FileEntity fileEntity = fileService.getFileEntity(fileId);
        Resource resource = fileService.previewById(fileId);
        String contentType = fileService.resolveMediaType(fileEntity.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("inline").filename(fileEntity.getName()).build().toString())
                .body(resource);
    }

    @GetMapping("/{clientId}/summary")
    @PreAuthorize("hasAuthority('client.view.all') or hasAuthority('client.view.own')")
    public ResponseEntity<ClientSummaryResponse> getClientSummary(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientService.getClientSummary(clientId));
    }

    @GetMapping("/{clientId}/info")
    @PreAuthorize("hasAuthority('client_info.create') or hasAuthority('client_info.view.all') or hasAuthority('client_info.view.own')")
    public ResponseEntity<ClientInfoHeaderResponse> getClientInfoHeader(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientService.getClientInfoHeader(clientId));
    }

    @GetMapping(value = "/{clientId}/info/{sectionKey}", produces = "application/json")
    @PreAuthorize("hasAuthority('client_info.create') or hasAuthority('client_info.view.all') or hasAuthority('client_info.view.own')")
    public ResponseEntity<String> getClientInfoSection(
            @PathVariable UUID clientId,
            @PathVariable String sectionKey
    ) {
        return ResponseEntity.ok(clientService.getClientInfoSection(clientId, sectionKey));
    }

    @PatchMapping("/{clientId}/info/{sectionKey}")
    @PreAuthorize("hasAuthority('client_info.create') or hasAuthority('client_info.edit')")
    public ResponseEntity<Void> updateClientInfoSection(
            @PathVariable UUID clientId,
            @PathVariable String sectionKey,
            @RequestBody JsonNode body
    ) {
        clientService.updateClientInfoSection(clientId, sectionKey, body);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{clientId}/activate")
    @PreAuthorize("hasAuthority('client.create')")
    public ResponseEntity<Void> activateClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientActivateRequest request
    ) {
        clientService.activateClient(clientId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{clientId}/archive-snapshot")
    @PreAuthorize("hasAuthority('client_info.view.own')")
    public ResponseEntity<ArchiveSnapshotResponse> getArchiveSnapshot(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientInfoTaskService.getArchiveSnapshot(clientId));
    }

    @PostMapping("/{clientId}/handoff")
    @PreAuthorize("hasAuthority('client.assign')")
    public ResponseEntity<Void> handoffClient(@PathVariable UUID clientId) {
        clientService.handoffClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{clientId}/status")
    @PreAuthorize("hasAuthority('client.manage')")
    public ResponseEntity<Void> updateClientStatus(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientStatusUpdateRequest request
    ) {
        clientService.updateClientStatus(clientId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAuthority('client.create')")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }
}

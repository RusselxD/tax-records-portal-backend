package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request.ClientInfoTaskCommentRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request.ProfileUpdateSubmitRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ClientInfoTaskResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileReviewListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.dto.response.ClientInfoLogItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client-info/tasks")
@RequiredArgsConstructor
public class ClientInfoTaskController {

    private final ClientInfoTaskService clientInfoTaskService;

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('client_info.create') or hasAuthority('client_info.review') or hasAuthority('client_info.edit')")
    public ResponseEntity<ClientInfoTaskResponse> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(clientInfoTaskService.getTask(taskId));
    }

    @PostMapping("/{clientId}/submit")
    @PreAuthorize("hasAuthority('client_info.create')")
    public ResponseEntity<Void> submitProfile(
            @PathVariable UUID clientId,
            @RequestBody(required = false) ClientInfoTaskCommentRequest request
    ) {
        clientInfoTaskService.submitProfile(clientId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{clientId}/submit-update")
    @PreAuthorize("hasAuthority('client_info.edit')")
    public ResponseEntity<Void> submitProfileUpdate(
            @PathVariable UUID clientId,
            @RequestBody(required = false) ProfileUpdateSubmitRequest request
    ) {
        clientInfoTaskService.submitProfileUpdate(clientId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/profile-update-review")
    @PreAuthorize("hasAuthority('client_info.review') or hasAuthority('client_info.edit')")
    public ResponseEntity<ProfileUpdateReviewResponse> getProfileUpdateReview(@PathVariable UUID taskId) {
        return ResponseEntity.ok(clientInfoTaskService.getProfileUpdateReview(taskId));
    }

    @PostMapping("/{taskId}/approve")
    @PreAuthorize("hasAuthority('client_info.review')")
    public ResponseEntity<Void> approveProfile(
            @PathVariable UUID taskId,
            @RequestBody(required = false) ClientInfoTaskCommentRequest request
    ) {
        clientInfoTaskService.approveProfile(taskId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("hasAuthority('client_info.review')")
    public ResponseEntity<Void> rejectProfile(
            @PathVariable UUID taskId,
            @RequestBody(required = false) ClientInfoTaskCommentRequest request
    ) {
        clientInfoTaskService.rejectProfile(taskId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/logs")
    @PreAuthorize("hasAuthority('client_info.create') or hasAuthority('client_info.review') or hasAuthority('client_info.view.all') or hasAuthority('client_info.edit')")
    public ResponseEntity<List<ClientInfoLogItemResponse>> getClientInfoTaskLogs(@PathVariable UUID taskId) {
        return ResponseEntity.ok(clientInfoTaskService.getLogsByTaskId(taskId));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('client_info.review')")
    public ResponseEntity<List<ProfileReviewListItemResponse>> getProfileReviews() {
        return ResponseEntity.ok(clientInfoTaskService.getProfileReviews());
    }
}

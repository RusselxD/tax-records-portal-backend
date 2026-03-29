package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log;

import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request.ConsultationLogActionRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request.ConsultationLogCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request.ConsultationLogUpdateRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogAuditCommentResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogAuditResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogDetailResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationMonthlySummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consultation-logs")
@RequiredArgsConstructor
public class ConsultationLogController {

    private final ConsultationLogService consultationLogService;

    @PostMapping
    @PreAuthorize("hasAuthority('consultation.create')")
    public ResponseEntity<ConsultationLogDetailResponse> create(
            @Valid @RequestBody ConsultationLogCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultationLogService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('consultation.create')")
    public ResponseEntity<ConsultationLogDetailResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ConsultationLogUpdateRequest request) {
        return ResponseEntity.ok(consultationLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('consultation.create')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        consultationLogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('consultation.view.own') or hasAuthority('consultation.view.all')")
    public ResponseEntity<ConsultationLogDetailResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(consultationLogService.getDetail(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('consultation.view.own') or hasAuthority('consultation.view.all')")
    public ResponseEntity<PageResponse<ConsultationLogListItemResponse>> list(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) ConsultationLogStatus status,
            @RequestParam(required = false) ConsultationBillableType billableType,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID createdById,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(consultationLogService.list(
                clientId, status, billableType, dateFrom, dateTo, search, createdById, page, size));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('consultation.create')")
    public ResponseEntity<Void> submit(@PathVariable UUID id,
                                        @RequestBody(required = false) ConsultationLogActionRequest request) {
        consultationLogService.submit(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('consultation.review')")
    public ResponseEntity<Void> approve(@PathVariable UUID id,
                                         @RequestBody(required = false) ConsultationLogActionRequest request) {
        consultationLogService.approve(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('consultation.review')")
    public ResponseEntity<Void> reject(@PathVariable UUID id,
                                        @RequestBody(required = false) ConsultationLogActionRequest request) {
        consultationLogService.reject(id, request);
        return ResponseEntity.noContent().build();
    }

    // --- Audit Logs ---

    @GetMapping("/{id}/audits")
    @PreAuthorize("hasAuthority('consultation.view.own') or hasAuthority('consultation.view.all')")
    public ResponseEntity<List<ConsultationLogAuditResponse>> getAudits(@PathVariable UUID id) {
        return ResponseEntity.ok(consultationLogService.getAudits(id));
    }

    @GetMapping("/{id}/audits/{auditId}/comment")
    @PreAuthorize("hasAuthority('consultation.view.own') or hasAuthority('consultation.view.all')")
    public ResponseEntity<ConsultationLogAuditCommentResponse> getAuditComment(
            @PathVariable UUID id, @PathVariable UUID auditId) {
        return ResponseEntity.ok(consultationLogService.getAuditComment(id, auditId));
    }

    // --- Summary ---

    @GetMapping("/client/{clientId}/summary")
    @PreAuthorize("hasAuthority('consultation.view.own') or hasAuthority('consultation.view.all')")
    public ResponseEntity<ConsultationMonthlySummaryResponse> getMonthlySummary(
            @PathVariable UUID clientId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(consultationLogService.getMonthlySummary(clientId, year, month));
    }

    @GetMapping("/me/summary")
    @PreAuthorize("hasAuthority('consultation.view.own.client')")
    public ResponseEntity<ConsultationMonthlySummaryResponse> getMyMonthlySummary() {
        return ResponseEntity.ok(consultationLogService.getMyMonthlySummary());
    }
}

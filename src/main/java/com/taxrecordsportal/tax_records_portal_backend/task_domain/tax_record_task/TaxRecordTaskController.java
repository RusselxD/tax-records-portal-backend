package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.bulk.BulkTaskCreateService;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.bulk.BulkTaskTemplateService;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.file.TaxRecordTaskFileService;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryService;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.AddWorkingLinkRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.BulkTaskRowRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.CreateTaxRecordTaskRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.TaskActionRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskFilesResponse.FileItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tax-record-tasks")
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class TaxRecordTaskController {

    private final TaxRecordTaskService taxRecordTaskService;
    private final TaxRecordTaskFileService taxRecordTaskFileService;
    private final BulkTaskCreateService bulkTaskCreateService;
    private final BulkTaskTemplateService bulkTaskTemplateService;
    private final TaxTaskCategoryService taxTaskCategoryService;

    // --- Dashboard ---

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(taxRecordTaskService.getDashboardStats());
    }

    @GetMapping("/reviewer-dashboard-stats")
    @PreAuthorize("hasAuthority('task.review')")
    public ResponseEntity<ReviewerDashboardStatsResponse> getReviewerDashboardStats() {
        return ResponseEntity.ok(taxRecordTaskService.getReviewerDashboardStats());
    }

    @GetMapping("/reviewer-queue")
    @PreAuthorize("hasAuthority('task.review')")
    public ResponseEntity<List<ReviewerQueueItemResponse>> getReviewerQueue() {
        return ResponseEntity.ok(taxRecordTaskService.getReviewerQueue());
    }

    @GetMapping("/recently-decided")
    @PreAuthorize("hasAuthority('task.review')")
    public ResponseEntity<List<ReviewerDecidedItemResponse>> getRecentlyDecided() {
        return ResponseEntity.ok(taxRecordTaskService.getRecentlyDecided());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<?> getMyOverdueTasks(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null) {
            return ResponseEntity.ok(taxRecordTaskService.getMyOverdueTasksPaged(page, size != null ? size : 20));
        }
        return ResponseEntity.ok(taxRecordTaskService.getMyOverdueTasks());
    }

    @GetMapping("/rejected")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<?> getMyRejectedTasks(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null) {
            return ResponseEntity.ok(taxRecordTaskService.getMyRejectedTasksPaged(page, size != null ? size : 20));
        }
        return ResponseEntity.ok(taxRecordTaskService.getMyRejectedTasks());
    }

    @GetMapping("/todo")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<PageResponse<TaxRecordTaskTodoListItemResponse>> getMyTodoTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taxRecordTaskService.getMyTodoTasks(page, size));
    }

    @GetMapping("/submitted")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<PageResponse<TaxRecordTaskProgressListItemResponse>> getMySubmittedTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taxRecordTaskService.getMyProgressTasks(TaxRecordTaskStatus.SUBMITTED, page, size));
    }

    @GetMapping("/for-filing")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<PageResponse<TaxRecordTaskProgressListItemResponse>> getMyForFilingTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taxRecordTaskService.getMyProgressTasks(TaxRecordTaskStatus.APPROVED_FOR_FILING, page, size));
    }

    @GetMapping("/filed")
    @PreAuthorize("hasAuthority('task.view.own')")
    public ResponseEntity<PageResponse<TaxRecordTaskProgressListItemResponse>> getMyFiledTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(taxRecordTaskService.getMyProgressTasks(TaxRecordTaskStatus.FILED, page, size));
    }


    // --- List & Detail ---

    @GetMapping
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own')")
    public ResponseEntity<PageResponse<TaxRecordTaskListItemResponse>> getTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) TaxRecordTaskStatus status,
            @RequestParam(required = false) UUID accountantId,
            @RequestParam(required = false) TaskSortField sortBy,
            @RequestParam(required = false) Sort.Direction sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(taxRecordTaskService.getTasks(
                search, clientId, period, status, accountantId,
                sortBy, sortDirection, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own')")
    public ResponseEntity<TaxRecordTaskDetailResponse> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(taxRecordTaskService.getTask(id));
    }

    // --- Client tasks (cursor-paginated) ---

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own')")
    public ResponseEntity<ClientTaxRecordTaskPageResponse> getClientTasks(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(taxRecordTaskService.getClientTasks(clientId, cursor));
    }

    // --- Files ---

    @GetMapping("/{id}/files")
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own')")
    public ResponseEntity<TaxRecordTaskFilesResponse> getFiles(@PathVariable UUID id) {
        return ResponseEntity.ok(taxRecordTaskFileService.getFiles(id));
    }

    @PostMapping("/{id}/working-files")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<WorkingFileItem> addWorkingFile(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(taxRecordTaskFileService.addWorkingFile(id, file));
    }

    @PostMapping("/{id}/working-links")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<WorkingFileItem> addWorkingLink(
            @PathVariable UUID id, @Valid @RequestBody AddWorkingLinkRequest request) {
        return ResponseEntity.ok(taxRecordTaskFileService.addWorkingLink(id, request));
    }

    @DeleteMapping("/{id}/working-files/{workingFileId}")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> removeWorkingFile(
            @PathVariable UUID id, @PathVariable UUID workingFileId) {
        taxRecordTaskFileService.removeWorkingFile(id, workingFileId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/output-file")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<FileItem> uploadOutputFile(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(taxRecordTaskFileService.uploadOutputFile(id, file));
    }

    @DeleteMapping("/{id}/output-file")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> deleteOutputFile(@PathVariable UUID id) {
        taxRecordTaskFileService.deleteOutputFile(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/proof-of-filing")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<FileItem> uploadProofOfFiling(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(taxRecordTaskFileService.uploadProofOfFiling(id, file));
    }

    @DeleteMapping("/{id}/proof-of-filing")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> deleteProofOfFiling(@PathVariable UUID id) {
        taxRecordTaskFileService.deleteProofOfFiling(id);
        return ResponseEntity.noContent().build();
    }

    // --- Status transitions ---

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> submit(@PathVariable UUID id, @RequestBody(required = false) TaskActionRequest request) {
        taxRecordTaskService.submit(id, request != null ? request.comment() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('task.review')")
    public ResponseEntity<Void> approve(@PathVariable UUID id, @RequestBody(required = false) TaskActionRequest request) {
        taxRecordTaskService.approve(id, request != null ? request.comment() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('task.review')")
    public ResponseEntity<Void> reject(@PathVariable UUID id, @RequestBody(required = false) TaskActionRequest request) {
        taxRecordTaskService.reject(id, request != null ? request.comment() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/recall")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> recall(@PathVariable UUID id) {
        taxRecordTaskService.recall(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-filed")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> markFiled(@PathVariable UUID id) {
        taxRecordTaskService.markFiled(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-completed")
    @PreAuthorize("hasAuthority('task.execute')")
    public ResponseEntity<Void> markCompleted(@PathVariable UUID id) {
        taxRecordTaskService.markCompleted(id);
        return ResponseEntity.noContent().build();
    }

    // --- Logs ---

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own')")
    public ResponseEntity<List<TaxRecordTaskLogResponse>> getLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(taxRecordTaskFileService.getLogs(id));
    }

    @GetMapping("/{id}/logs/{logId}/comment")
    @PreAuthorize("hasAuthority('task.view.all') or hasAuthority('task.view.own')")
    public ResponseEntity<TaxRecordTaskLogCommentResponse> getLogComment(
            @PathVariable UUID id, @PathVariable UUID logId) {
        return ResponseEntity.ok(taxRecordTaskFileService.getLogComment(id, logId));
    }

    // --- Create ---

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taxRecordTaskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<CreateTaxRecordTaskResponse> createTask(
            @Valid @RequestBody CreateTaxRecordTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRecordTaskService.createTask(request));
    }

    // --- Bulk ---

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<BulkTaskCreateResponse> bulkCreateTasks(
            @Valid @NotEmpty @Size(max = 500, message = "Cannot import more than 500 tasks at once.")
            @RequestBody List<BulkTaskRowRequest> rows) {
        return ResponseEntity.ok(bulkTaskCreateService.bulkCreateTasks(rows));
    }

    @GetMapping("/bulk-template")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<byte[]> getBulkTemplate() throws IOException {
        byte[] file = bulkTaskTemplateService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bulk_task_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    // --- Lookup Hierarchy ---

    @GetMapping("/lookup-hierarchy")
    @PreAuthorize("hasAuthority('task.create')")
    public ResponseEntity<List<LookupHierarchyResponse>> getLookupHierarchy() {
        return ResponseEntity.ok(taxTaskCategoryService.getHierarchy());
    }
}

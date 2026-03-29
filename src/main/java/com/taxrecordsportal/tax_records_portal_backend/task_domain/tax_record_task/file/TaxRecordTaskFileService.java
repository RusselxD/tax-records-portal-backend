package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.file;

import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTask;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskAccessHelper;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileService;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.dto.FileUploadResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.AddWorkingLinkRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskFilesResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskFilesResponse.FileItem;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskLogCommentResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskLogResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log.TaxRecordTaskLogRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class TaxRecordTaskFileService {

    private static final Set<TaxRecordTaskStatus> WORKING_FILE_STATUSES = Set.of(
            TaxRecordTaskStatus.OPEN, TaxRecordTaskStatus.REJECTED);
    private static final Set<TaxRecordTaskStatus> PROOF_STATUSES = Set.of(
            TaxRecordTaskStatus.APPROVED_FOR_FILING, TaxRecordTaskStatus.FILED);

    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final FileService fileService;
    private final TaxRecordTaskLogRepository taxRecordTaskLogRepository;
    private final TaxRecordTaskAccessHelper accessHelper;

    // --- GET files ---

    @Transactional(readOnly = true)
    public TaxRecordTaskFilesResponse getFiles(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        accessHelper.enforceViewAccess(task);
        return toFilesResponse(task);
    }

    // --- Working files: upload ---

    @Transactional
    public WorkingFileItem addWorkingFile(UUID taskId, MultipartFile file) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, WORKING_FILE_STATUSES);

        FileUploadResponse uploaded = fileService.upload(task.getClient().getId(), file);

        WorkingFileItem item = new WorkingFileItem("file", uploaded.id(), uploaded.name(), null, null);
        List<WorkingFileItem> files = task.getWorkingFiles() != null
                ? new ArrayList<>(task.getWorkingFiles()) : new ArrayList<>();
        files.add(item);
        task.setWorkingFiles(files);
        taxRecordTaskRepository.save(task);
        return item;
    }

    // --- Working files: add link ---

    @Transactional
    public WorkingFileItem addWorkingLink(UUID taskId, AddWorkingLinkRequest request) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, WORKING_FILE_STATUSES);

        WorkingFileItem item = new WorkingFileItem("link", null, null, request.url(), request.label());
        List<WorkingFileItem> files = task.getWorkingFiles() != null
                ? new ArrayList<>(task.getWorkingFiles()) : new ArrayList<>();
        files.add(item);
        task.setWorkingFiles(files);
        taxRecordTaskRepository.save(task);
        return item;
    }

    // --- Working files: delete ---

    @Transactional
    public void removeWorkingFile(UUID taskId, UUID workingFileId) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, WORKING_FILE_STATUSES);

        List<WorkingFileItem> files = task.getWorkingFiles();
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Working file not found");
        }

        WorkingFileItem target = files.stream()
                .filter(f -> "file".equals(f.type()) && workingFileId.equals(f.fileId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Working file not found"));

        // Delete the actual file from storage
        fileService.delete(target.fileId());

        files.remove(target);
        task.setWorkingFiles(files);
        taxRecordTaskRepository.save(task);
    }

    // --- Working files: delete link ---

    @Transactional
    public void removeWorkingLink(UUID taskId, int index) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, WORKING_FILE_STATUSES);

        List<WorkingFileItem> files = task.getWorkingFiles();
        if (files == null || index < 0 || index >= files.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Working file not found");
        }

        WorkingFileItem target = files.get(index);
        if (!"link".equals(target.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item at index is not a link");
        }

        files.remove(index);
        task.setWorkingFiles(files);
        taxRecordTaskRepository.save(task);
    }

    // --- Output file ---

    @Transactional
    public FileItem uploadOutputFile(UUID taskId, MultipartFile file) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, WORKING_FILE_STATUSES);

        // Delete old output file if exists
        if (task.getOutputFile() != null) {
            fileService.delete(task.getOutputFile().getId());
        }

        FileUploadResponse uploaded = fileService.upload(task.getClient().getId(), file);
        FileEntity fileEntity = fileService.getFileEntity(uploaded.id());
        task.setOutputFile(fileEntity);
        taxRecordTaskRepository.save(task);
        return new FileItem(fileEntity.getId(), fileEntity.getName());
    }

    @Transactional
    public void deleteOutputFile(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, WORKING_FILE_STATUSES);

        if (task.getOutputFile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No output file to delete");
        }

        fileService.delete(task.getOutputFile().getId());
        task.setOutputFile(null);
        taxRecordTaskRepository.save(task);
    }

    // --- Proof of filing ---

    @Transactional
    public FileItem uploadProofOfFiling(UUID taskId, MultipartFile file) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, PROOF_STATUSES);

        if (task.getProofOfFilingFile() != null) {
            fileService.delete(task.getProofOfFilingFile().getId());
        }

        FileUploadResponse uploaded = fileService.upload(task.getClient().getId(), file);
        FileEntity fileEntity = fileService.getFileEntity(uploaded.id());
        task.setProofOfFilingFile(fileEntity);
        taxRecordTaskRepository.save(task);
        return new FileItem(fileEntity.getId(), fileEntity.getName());
    }

    @Transactional
    public void deleteProofOfFiling(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        enforceExecuteAccess(task, PROOF_STATUSES);

        if (task.getProofOfFilingFile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No proof of filing file to delete");
        }

        fileService.delete(task.getProofOfFilingFile().getId());
        task.setProofOfFilingFile(null);
        taxRecordTaskRepository.save(task);
    }

    // --- Logs ---

    @Transactional(readOnly = true)
    public List<TaxRecordTaskLogResponse> getLogs(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskForFileOps(taskId);
        accessHelper.enforceViewAccess(task);
        return taxRecordTaskLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(log -> new TaxRecordTaskLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getComment() != null && !log.getComment().isEmpty(),
                        UserDisplayUtil.formatDisplayName(log.getPerformedBy()),
                        log.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxRecordTaskLogCommentResponse getLogComment(UUID taskId, UUID logId) {
        accessHelper.enforceViewAccess(accessHelper.findTaskForFileOps(taskId));
        var log = taxRecordTaskLogRepository.findById(logId)
                .filter(l -> l.getTask().getId().equals(taskId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log not found"));
        return new TaxRecordTaskLogCommentResponse(log.getId(), log.getComment());
    }

    // --- Helpers ---

    private void enforceExecuteAccess(TaxRecordTask task, Set<TaxRecordTaskStatus> allowedStatuses) {
        User currentUser = getCurrentUser();

        if (!accessHelper.hasPermission(currentUser, "task.execute")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to modify task files");
        }

        accessHelper.enforceAssigned(task, currentUser);

        if (!allowedStatuses.contains(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot modify files in status: " + task.getStatus());
        }
    }

    private TaxRecordTaskFilesResponse toFilesResponse(TaxRecordTask task) {
        List<WorkingFileItem> workingFiles = task.getWorkingFiles() != null
                ? task.getWorkingFiles() : List.of();

        FileItem outputFile = task.getOutputFile() != null
                ? new FileItem(task.getOutputFile().getId(), task.getOutputFile().getName())
                : null;

        FileItem proofOfFiling = task.getProofOfFilingFile() != null
                ? new FileItem(task.getProofOfFilingFile().getId(), task.getProofOfFilingFile().getName())
                : null;

        return new TaxRecordTaskFilesResponse(workingFiles, outputFile, proofOfFiling);
    }
}

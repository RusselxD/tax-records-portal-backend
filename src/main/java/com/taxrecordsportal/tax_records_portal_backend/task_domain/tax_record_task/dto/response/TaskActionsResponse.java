package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

public record TaskActionsResponse(
        boolean canEdit,
        boolean canSubmit,
        boolean canRecall,
        boolean canApprove,
        boolean canReject,
        boolean canMarkFiled,
        boolean canMarkCompleted,
        boolean canUploadWorkingFiles,
        boolean canUploadOutput,
        boolean canUploadProof,
        boolean canDelete
) {}

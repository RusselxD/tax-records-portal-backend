package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTaxRecordLookupRequest(@NotBlank String name) {}

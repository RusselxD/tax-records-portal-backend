package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationBillableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConsultationLogCreateRequest(
        @NotNull UUID clientId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String platform,
        @NotBlank String subject,
        Map<String, Object> notes,
        List<FileReference> attachments,
        ConsultationBillableType billableType
) {}

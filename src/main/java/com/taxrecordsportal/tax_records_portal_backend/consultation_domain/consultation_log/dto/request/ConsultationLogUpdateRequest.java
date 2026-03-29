package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationBillableType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record ConsultationLogUpdateRequest(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String platform,
        String subject,
        Map<String, Object> notes,
        List<FileReference> attachments,
        ConsultationBillableType billableType
) {}

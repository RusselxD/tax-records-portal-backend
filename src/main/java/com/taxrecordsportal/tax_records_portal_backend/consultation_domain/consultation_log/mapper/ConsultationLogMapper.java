package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.mapper;

import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationLog;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogAuditResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogDetailResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationMonthlySummaryResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit.ConsultationLogAudit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class ConsultationLogMapper {

    public ConsultationLogListItemResponse toListItem(ConsultationLog log, String clientDisplayName) {
        return new ConsultationLogListItemResponse(
                log.getId(),
                log.getClient().getId(),
                clientDisplayName,
                log.getDate(),
                log.getStartTime(),
                log.getEndTime(),
                log.getHours(),
                log.getPlatform(),
                log.getSubject(),
                log.getBillableType(),
                log.getStatus(),
                UserDisplayUtil.formatDisplayName(log.getCreatedBy()),
                log.getCreatedAt()
        );
    }

    public ConsultationLogDetailResponse toDetail(ConsultationLog log, String clientDisplayName) {
        List<ConsultationLogDetailResponse.FileItemResponse> attachments = log.getAttachments() != null
                ? log.getAttachments().stream()
                    .map(f -> new ConsultationLogDetailResponse.FileItemResponse(f.id(), f.name()))
                    .toList()
                : Collections.emptyList();

        return new ConsultationLogDetailResponse(
                log.getId(),
                log.getClient().getId(),
                clientDisplayName,
                log.getDate(),
                log.getStartTime(),
                log.getEndTime(),
                log.getHours(),
                log.getPlatform(),
                log.getSubject(),
                log.getNotes(),
                attachments,
                log.getBillableType(),
                log.getStatus(),
                log.getCreatedBy().getId(),
                UserDisplayUtil.formatDisplayName(log.getCreatedBy()),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }

    public ConsultationLogAuditResponse toAuditListItem(ConsultationLogAudit audit) {
        return new ConsultationLogAuditResponse(
                audit.getId(),
                audit.getAction(),
                audit.getComment() != null && !audit.getComment().isEmpty(),
                UserDisplayUtil.formatDisplayName(audit.getPerformedBy()),
                audit.getCreatedAt()
        );
    }

    public ConsultationMonthlySummaryResponse toMonthlySummary(
            int year, int month,
            BigDecimal totalHoursConsumed, BigDecimal courtesyHours,
            BigDecimal includedHours, BigDecimal excessRate) {

        BigDecimal billableHours = totalHoursConsumed.subtract(courtesyHours);
        BigDecimal remainingIncluded = includedHours.subtract(billableHours).max(BigDecimal.ZERO);
        BigDecimal excessHours = billableHours.subtract(includedHours).max(BigDecimal.ZERO);
        BigDecimal estimatedExcessFee = excessHours.multiply(excessRate);

        return new ConsultationMonthlySummaryResponse(
                year, month,
                totalHoursConsumed, courtesyHours, billableHours,
                includedHours, remainingIncluded, excessHours,
                excessRate, estimatedExcessFee
        );
    }
}

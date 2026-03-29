package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationLogAuditRepository extends JpaRepository<ConsultationLogAudit, UUID> {

    @EntityGraph(attributePaths = {"performedBy"})
    List<ConsultationLogAudit> findByConsultationLogIdOrderByCreatedAtDesc(UUID consultationLogId);
}

package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit;

import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.ConsultationLog;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "consultation_log_audits", indexes = {
        @Index(name = "idx_cla_log_id", columnList = "consultation_log_id")
})
public class ConsultationLogAudit {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "consultation_log_id", nullable = false)
    private ConsultationLog consultationLog;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationLogAuditAction action;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> comment;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

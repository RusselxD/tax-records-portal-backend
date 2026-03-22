package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTask;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tax_record_task_logs", indexes = {
        @Index(name = "idx_trt_logs_task_id", columnList = "task_id"),
        @Index(name = "idx_trt_logs_performed_by_created", columnList = "performed_by, created_at"),
        @Index(name = "idx_trt_logs_action_created", columnList = "action, created_at")
})
public class TaxRecordTaskLog {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaxRecordTask task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxRecordTaskLogAction action;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

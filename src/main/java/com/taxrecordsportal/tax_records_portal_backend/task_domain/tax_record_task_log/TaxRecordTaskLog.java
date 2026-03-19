package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTask;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tax_record_task_logs")
public class TaxRecordTaskLog {

    @Id
    @GeneratedValue(strategy = UUID)
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

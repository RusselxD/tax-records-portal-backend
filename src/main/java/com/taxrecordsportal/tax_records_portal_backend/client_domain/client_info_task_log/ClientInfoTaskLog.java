package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTask;
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
@Table(name = "client_info_task_logs", indexes = {
        @Index(name = "idx_cit_logs_task_id", columnList = "task_id")
})
public class ClientInfoTaskLog {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ClientInfoTask task;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ClientInfoTaskLogAction action;

    @Column(name = "comment")
    private String comment;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

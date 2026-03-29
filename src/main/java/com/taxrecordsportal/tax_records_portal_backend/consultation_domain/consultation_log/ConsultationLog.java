package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
@Table(name = "consultation_logs", indexes = {
        @Index(name = "idx_cl_client_id", columnList = "client_id"),
        @Index(name = "idx_cl_status", columnList = "status"),
        @Index(name = "idx_cl_client_status", columnList = "client_id, status"),
        @Index(name = "idx_cl_client_date", columnList = "client_id, date"),
        @Index(name = "idx_cl_created_by", columnList = "created_by")
})
public class ConsultationLog {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private java.util.UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal hours;

    @Column(name = "platform")
    private String platform;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "notes", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> notes;

    @Column(name = "attachments", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<FileReference> attachments;

    @Enumerated(EnumType.STRING)
    @Column(name = "billable_type", nullable = false)
    private ConsultationBillableType billableType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsultationLogStatus status = ConsultationLogStatus.DRAFT;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

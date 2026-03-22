package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tax_record_tasks", indexes = {
        @Index(name = "idx_trt_client_id", columnList = "client_id"),
        @Index(name = "idx_trt_status", columnList = "status"),
        @Index(name = "idx_trt_deadline", columnList = "deadline"),
        @Index(name = "idx_trt_created_by", columnList = "created_by"),
        @Index(name = "idx_trt_created_at", columnList = "created_at"),
        @Index(name = "idx_trt_status_deadline", columnList = "status, deadline"),
        @Index(name = "idx_trt_client_status", columnList = "client_id, status"),
        @Index(name = "idx_trt_client_status_deadline", columnList = "client_id, status, deadline")
})
public class TaxRecordTask {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TaxTaskCategory category;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private TaxTaskSubCategory subCategory;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "task_name_id", nullable = false)
    private TaxTaskName taskName;

    @Column(nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;

    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxRecordTaskStatus status;

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<WorkingFileItem> workingFiles = new java.util.ArrayList<>();

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "output_file_id", nullable = true)
    private FileEntity outputFile;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "proof_of_filing_file_id", nullable = true)
    private FileEntity proofOfFilingFile;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "tax_record_task_accountants",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            indexes = {
                    @Index(name = "idx_task_accountants_task_id", columnList = "task_id"),
                    @Index(name = "idx_task_accountants_user_id", columnList = "user_id")
            }
    )
    @BatchSize(size = 20)
    private Set<User> assignedTo;

    // Computed from drill-down path: "Category > Sub Category > Task Name > Year > Period"
    @Transient
    public String getTitle() {
        return String.join(" > ",
                category != null ? category.getName() : "",
                subCategory != null ? subCategory.getName() : "",
                taskName != null ? taskName.getName() : "",
                String.valueOf(year),
                period != null ? period.name() : ""
        );
    }
}

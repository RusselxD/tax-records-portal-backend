package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
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
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tax_record_entries", indexes = {
        @Index(name = "idx_tre_client_id", columnList = "client_id")
})
public class TaxRecordEntry {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private UUID id;

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

    @Column(name = "year", nullable = false)
    private int year;

    @Enumerated(STRING)
    @Column(name = "period", nullable = false)
    private Period period;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_files", columnDefinition = "jsonb")
    private List<WorkingFileItem> workingFiles;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "output_file_id")
    private FileEntity outputFile;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "proof_of_filing_file_id")
    private FileEntity proofOfFilingFile;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

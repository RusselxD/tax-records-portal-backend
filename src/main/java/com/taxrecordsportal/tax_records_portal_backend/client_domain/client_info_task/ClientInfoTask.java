package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.SectionDiff;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "client_info_tasks", indexes = {
        @Index(name = "idx_cit_client_type_status", columnList = "client_id, type, status"),
        @Index(name = "idx_cit_submitted_at", columnList = "submitted_at"),
        @Index(name = "idx_cit_submitted_by", columnList = "submitted_by")
})
public class ClientInfoTask {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ClientInfoTaskType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClientInfoTaskStatus status;

    // Snapshot of client info at submission time (7 JSONB columns)
    @Column(name = "main_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private MainDetails mainDetails;

    @Column(name = "client_information", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private ClientInformation clientInformation;

    @Column(name = "corporate_officer_information", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private CorporateOfficerInformation corporateOfficerInformation;

    @Column(name = "access_credentials", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<AccessCredentialDetails> accessCredentials;

    @Column(name = "scope_of_engagement", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private ScopeOfEngagementDetails scopeOfEngagement;

    @Column(name = "professional_fees", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProfessionalFeeEntry> professionalFees;

    @Column(name = "onboarding_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private OnboardingDetails onboardingDetails;

    @Column(name = "changed_section_keys", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> changedSectionKeys;

    @Column(name = "approved_diff", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<SectionDiff> approvedDiff;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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

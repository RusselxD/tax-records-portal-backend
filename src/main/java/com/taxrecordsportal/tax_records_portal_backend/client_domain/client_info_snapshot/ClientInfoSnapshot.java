package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_info_snapshots")
public class ClientInfoSnapshot {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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
}

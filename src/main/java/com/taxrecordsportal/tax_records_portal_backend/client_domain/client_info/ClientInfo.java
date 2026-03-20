package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_info", indexes = {
        @Index(name = "idx_client_info_client_id", columnList = "client_id")
})
public class ClientInfo {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

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

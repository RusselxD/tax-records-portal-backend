package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_info_templates")
public class ClientInfoTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "main_details", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private MainDetails mainDetails;

    @Column(name = "client_information", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private ClientInformation clientInformation;

    @Column(name = "corporate_officer_information", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private CorporateOfficerInformation corporateOfficerInformation;

    @Column(name = "access_credentials", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<AccessCredentialDetails> accessCredentials;

    @Column(name = "scope_of_engagement", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private ScopeOfEngagementDetails scopeOfEngagement;

    @Column(name = "professional_fees", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProfessionalFeeEntry> professionalFees;

    @Column(name = "onboarding_details", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private OnboardingDetails onboardingDetails;
}

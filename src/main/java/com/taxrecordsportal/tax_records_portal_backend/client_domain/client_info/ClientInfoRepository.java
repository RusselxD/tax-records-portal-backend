package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientInfoRepository extends JpaRepository<ClientInfo, UUID> {
    Optional<ClientInfo> findByClientId(UUID clientId);
    void deleteByClientId(UUID clientId);

    @Query(value = "SELECT main_details FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findMainDetailsByClientId(UUID clientId);

    @Query(value = "SELECT client_information FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findClientInformationByClientId(UUID clientId);

    @Query(value = "SELECT corporate_officer_information FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findCorporateOfficerInformationByClientId(UUID clientId);

    @Query(value = "SELECT access_credentials FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findAccessCredentialsByClientId(UUID clientId);

    @Query(value = "SELECT scope_of_engagement FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findScopeOfEngagementByClientId(UUID clientId);

    @Query(value = "SELECT professional_fees FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findProfessionalFeesByClientId(UUID clientId);

    @Query(value = "SELECT onboarding_details FROM client_info WHERE client_id = :clientId", nativeQuery = true)
    String findOnboardingDetailsByClientId(UUID clientId);

    @Query(value = """
            SELECT (ci.scope_of_engagement -> 'engagementLetters') IS NOT NULL
              AND jsonb_array_length(COALESCE(ci.scope_of_engagement -> 'engagementLetters', '[]'::jsonb)) > 0
            FROM client_info ci
            JOIN users u ON u.client_id = ci.client_id
            WHERE u.id = :userId
            """, nativeQuery = true)
    Optional<Boolean> existsEngagementLetterByUserId(UUID userId);

    @Query(value = """
            SELECT jsonb_array_elements(ci.scope_of_engagement -> 'engagementLetters') ->> 'id'
            FROM client_info ci
            JOIN users u ON u.client_id = ci.client_id
            WHERE u.id = :userId
            """, nativeQuery = true)
    List<String> findEngagementLetterFileIdsByUserId(UUID userId);
}

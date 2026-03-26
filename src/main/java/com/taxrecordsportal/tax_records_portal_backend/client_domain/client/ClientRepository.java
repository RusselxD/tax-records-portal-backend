package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.ClientStatusCountProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemClientStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    @Override
    @EntityGraph(attributePaths = {"clientInfo"})
    Page<Client> findAll(@NonNull Specification<Client> spec, @NonNull Pageable pageable);

    Long countByStatus(ClientStatus status);

    @EntityGraph(attributePaths = {"users", "clientInfo", "accountants"})
    List<Client> findByCreatedById(UUID createdById);

    @EntityGraph(attributePaths = {"createdBy", "accountants", "clientInfo"})
    List<Client> findByStatusNot(ClientStatus status);

    @EntityGraph(attributePaths = {"createdBy", "clientInfo"})
    Optional<Client> findWithInfoAndCreatorById(UUID id);

    Long countByStatusNot(ClientStatus clientStatus);

    @EntityGraph(attributePaths = {"createdBy", "accountants"})
    Optional<Client> findWithCreatorAndAccountantsById(UUID id);

    @EntityGraph(attributePaths = {"users", "createdBy"})
    Optional<Client> findWithUsersAndCreatorById(UUID id);

    @EntityGraph(attributePaths = {"createdBy", "accountants", "users"})
    Optional<Client> findWithCreatorAccountantsAndUsersById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo"})
    List<Client> findByAccountantsId(UUID userId);

    @EntityGraph(attributePaths = {"clientInfo"})
    Page<Client> findPageByAccountantsId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"clientInfo", "accountants"})
    List<Client> findByAccountantsIdAndStatusNot(UUID userId, ClientStatus status);

    @EntityGraph(attributePaths = {"clientInfo", "accountants", "accountants.role"})
    List<Client> findByStatusAndAccountantsIsNotEmpty(ClientStatus status);

    @EntityGraph(attributePaths = {"accountants"})
    Optional<Client> findWithAccountantsById(UUID id);

    @EntityGraph(attributePaths = {"users"})
    Optional<Client> findWithUsersById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo", "users"})
    Optional<Client> findWithInfoAndUsersById(UUID id);

    @EntityGraph(attributePaths = {"accountants", "users"})
    Optional<Client> findWithAccountantsAndUsersById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo", "accountants", "users"})
    Optional<Client> findWithInfoAccountantsAndUsersById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo", "accountants"})
    Optional<Client> findWithInfoAndAccountantsById(UUID id);

    @EntityGraph(attributePaths = {"createdBy", "clientInfo", "accountants"})
    Optional<Client> findWithInfoCreatorAndAccountantsById(UUID id);

    @Query("SELECT u.client.id FROM User u WHERE u.id = :userId AND u.client IS NOT NULL")
    Optional<UUID> findClientIdByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"accountants"})
    List<Client> findByIdIn(List<UUID> ids);

    @Query(nativeQuery = true, value = """
            SELECT c.status AS status, COUNT(c.id) AS count
            FROM clients c
            JOIN client_accountants ca ON ca.client_id = c.id
            WHERE ca.user_id = :userId
            GROUP BY c.status
            """)
    List<ClientStatusCountProjection> countClientsByStatusForUser(@Param("userId") UUID userId);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE c.status = 'ONBOARDING') AS onboarding,
                   COUNT(*) FILTER (WHERE c.status = 'ACTIVE_CLIENT') AS active,
                   COUNT(*) FILTER (WHERE c.status = 'OFFBOARDING') AS offboarding,
                   COUNT(*) FILTER (WHERE c.status = 'INACTIVE_CLIENT') AS inactive
            FROM clients c
            """)
    SystemClientStatsProjection findSystemClientStats();
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.ClientStatusCountProjection;
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
    @EntityGraph(attributePaths = {"clientInfo", "accountants"})
    Page<Client> findAll(@NonNull Specification<Client> spec, @NonNull Pageable pageable);

    Long countByStatus(ClientStatus status);

    @EntityGraph(attributePaths = {"user", "clientInfo", "accountants"})
    List<Client> findByCreatedById(UUID createdById);

    @EntityGraph(attributePaths = {"createdBy", "accountants", "clientInfo"})
    List<Client> findByStatusNot(ClientStatus status);

    @EntityGraph(attributePaths = {"createdBy", "clientInfo"})
    Optional<Client> findWithInfoAndCreatorById(UUID id);

    Long countByStatusNot(ClientStatus clientStatus);

    boolean existsByIdAndUserIsNotNull(UUID id);

    @EntityGraph(attributePaths = {"createdBy", "accountants"})
    Optional<Client> findWithCreatorAndAccountantsById(UUID id);

    @EntityGraph(attributePaths = {"user", "createdBy"})
    Optional<Client> findWithUserAndCreatorById(UUID id);

    @EntityGraph(attributePaths = {"createdBy", "accountants", "user"})
    Optional<Client> findWithCreatorAccountantsAndUserById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo"})
    List<Client> findByAccountantsId(UUID userId);

    @EntityGraph(attributePaths = {"clientInfo"})
    Page<Client> findPageByAccountantsId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"clientInfo", "accountants"})
    List<Client> findByAccountantsIdAndStatusNot(UUID userId, ClientStatus status);

    @EntityGraph(attributePaths = {"clientInfo", "accountants"})
    List<Client> findByStatusAndAccountantsIsNotEmpty(ClientStatus status);

    @EntityGraph(attributePaths = {"accountants"})
    Optional<Client> findWithAccountantsById(UUID id);

    @EntityGraph(attributePaths = {"accountants", "user"})
    Optional<Client> findWithAccountantsAndUserById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo", "accountants", "user"})
    Optional<Client> findWithInfoAccountantsAndUserById(UUID id);

    @EntityGraph(attributePaths = {"clientInfo", "accountants"})
    Optional<Client> findWithInfoAndAccountantsById(UUID id);

    @EntityGraph(attributePaths = {"createdBy", "clientInfo", "accountants"})
    Optional<Client> findWithInfoCreatorAndAccountantsById(UUID id);

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
}

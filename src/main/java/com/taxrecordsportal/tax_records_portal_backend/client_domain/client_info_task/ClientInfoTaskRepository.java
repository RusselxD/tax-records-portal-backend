package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemProfileStatsProjection;
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
import java.util.Set;
import java.util.UUID;

public interface ClientInfoTaskRepository extends JpaRepository<ClientInfoTask, UUID>, JpaSpecificationExecutor<ClientInfoTask> {

    @Override
    @EntityGraph(attributePaths = {"client", "submittedBy"})
    @NonNull
    Page<ClientInfoTask> findAll(@NonNull Specification<ClientInfoTask> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"client", "client.clientInfo", "submittedBy"})
    List<ClientInfoTask> findAllByOrderBySubmittedAtDesc();

    boolean existsByClientIdAndStatusIn(UUID clientId, List<ClientInfoTaskStatus> statuses);

    boolean existsByClientIdAndTypeAndStatus(UUID clientId, ClientInfoTaskType type, ClientInfoTaskStatus status);

    Optional<ClientInfoTask> findByClientIdAndStatus(UUID clientId, ClientInfoTaskStatus status);

    @EntityGraph(attributePaths = {"client", "client.clientInfo", "submittedBy"})
    Optional<ClientInfoTask> findWithClientInfoAndSubmitterByClientIdAndStatus(UUID clientId, ClientInfoTaskStatus status);

    Optional<ClientInfoTask> findByClientIdAndStatusIn(UUID clientId, List<ClientInfoTaskStatus> statuses);

    @Query("SELECT t.client.id FROM ClientInfoTask t WHERE t.status = 'SUBMITTED'")
    Set<UUID> findClientIdsWithActiveTask();

    Optional<ClientInfoTask> findTopByClientIdOrderBySubmittedAtDesc(UUID clientId);

    Optional<ClientInfoTask> findTopByClientIdAndTypeOrderBySubmittedAtDesc(UUID clientId, ClientInfoTaskType type);

    // fetches the latest task for each type in one query — avoids separate exists + find calls
    @Query("""
            SELECT t FROM ClientInfoTask t
            WHERE t.client.id = :clientId
              AND t.submittedAt = (
                  SELECT MAX(t2.submittedAt) FROM ClientInfoTask t2
                  WHERE t2.client.id = :clientId AND t2.type = t.type
              )
            """)
    List<ClientInfoTask> findLatestByClientIdGroupedByType(@Param("clientId") UUID clientId);

    @EntityGraph(attributePaths = {"client", "client.clientInfo", "submittedBy"})
    Optional<ClientInfoTask> findWithClientInfoAndSubmitterById(UUID taskId);

    @EntityGraph(attributePaths = {"submittedBy"})
    Optional<ClientInfoTask> findWithSubmitterById(UUID taskId);

    @EntityGraph(attributePaths = {"client", "client.clientInfo", "client.accountants", "client.accountants.role", "client.user", "submittedBy"})
    Optional<ClientInfoTask> findWithFullClientDetailsById(UUID taskId);

    @EntityGraph(attributePaths = {"client", "client.accountants", "client.accountants.role"})
    Optional<ClientInfoTask> findByClientIdAndType(UUID clientId, ClientInfoTaskType type);

    @EntityGraph(attributePaths = {"client", "client.clientInfo", "submittedBy"})
    List<ClientInfoTask> findByClient_Accountants_IdOrderBySubmittedAtDesc(UUID accountantId);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE type = 'ONBOARDING') AS onboarding,
                   COUNT(*) FILTER (WHERE type = 'PROFILE_UPDATE') AS updates
            FROM client_info_tasks
            WHERE status = 'SUBMITTED'
            """)
    SystemProfileStatsProjection findSystemProfileStats();
}

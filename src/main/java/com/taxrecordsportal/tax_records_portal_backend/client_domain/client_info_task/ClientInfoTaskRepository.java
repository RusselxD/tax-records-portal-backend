package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ClientInfoTaskRepository extends JpaRepository<ClientInfoTask, UUID> {

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
}

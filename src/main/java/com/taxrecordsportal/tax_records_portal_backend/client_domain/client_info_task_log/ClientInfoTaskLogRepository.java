package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientInfoTaskLogRepository extends JpaRepository<ClientInfoTaskLog, UUID> {
    List<ClientInfoTaskLog> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    @EntityGraph(attributePaths = {"performedBy"})
    List<ClientInfoTaskLog> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    @EntityGraph(attributePaths = {"performedBy"})
    List<ClientInfoTaskLog> findByTaskClientIdOrderByCreatedAtDesc(UUID clientId);

    Optional<ClientInfoTaskLog> findTopByTaskIdAndActionInOrderByCreatedAtDesc(UUID taskId, List<ClientInfoTaskLogAction> actions);
}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.CategoryStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.TaskSummaryProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ClientDisplayNameProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ClientTaskMetrics;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.DashboardStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ReviewerTaskStatsProjection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxRecordTaskRepository extends JpaRepository<TaxRecordTask, UUID>, JpaSpecificationExecutor<TaxRecordTask> {

    @Override
    @EntityGraph(attributePaths = {"client", "category", "subCategory", "taskName", "createdBy"})
    Page<TaxRecordTask> findAll(@NonNull Specification<TaxRecordTask> spec, @NonNull Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT c.id AS clientId,
                   ci.client_information->>'registeredName' AS registeredName,
                   ci.client_information->>'tradeName' AS tradeName
            FROM clients c
            LEFT JOIN client_info ci ON ci.client_id = c.id
            WHERE c.id IN (:clientIds)
            """)
    List<ClientDisplayNameProjection> findClientDisplayNamesByIds(@Param("clientIds") List<UUID> clientIds);

    @Query("SELECT t.id, a FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.id IN :taskIds")
    List<Object[]> findAssignedUsersByTaskIds(@Param("taskIds") List<UUID> taskIds);

    @EntityGraph(attributePaths = {"client.clientInfo", "assignedTo", "category", "subCategory", "taskName"})
    @Query("SELECT t FROM TaxRecordTask t")
    List<TaxRecordTask> findAllWithDetails();

    @EntityGraph(attributePaths = {"category", "subCategory", "taskName"})
    List<TaxRecordTask> findByClientIdIn(List<UUID> clientIds);

    @Query(nativeQuery = true, value = """
            SELECT CONCAT(t.client_id, '::', LOWER(c.name), '::', LOWER(sc.name), '::', LOWER(tn.name), '::', t.year, '::', t.period, '::', CAST(t.deadline AS date))
            FROM tax_record_tasks t
            JOIN tax_task_categories c ON c.id = t.category_id
            JOIN tax_task_sub_categories sc ON sc.id = t.sub_category_id
            JOIN tax_task_names tn ON tn.id = t.task_name_id
            WHERE t.client_id IN (:clientIds)
            """)
    List<String> findTaskKeysByClientIds(@Param("clientIds") List<UUID> clientIds);

    @EntityGraph(attributePaths = {"client.clientInfo", "assignedTo", "category", "subCategory", "taskName"})
    List<TaxRecordTask> findByAssignedTo_Id(UUID userId);

    @EntityGraph(attributePaths = {"client.clientInfo", "client.accountants", "assignedTo", "category", "subCategory", "taskName", "createdBy"})
    @Query("SELECT t FROM TaxRecordTask t WHERE t.id = :id")
    Optional<TaxRecordTask> findWithDetailsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"client", "assignedTo", "outputFile", "proofOfFilingFile"})
    @Query("SELECT t FROM TaxRecordTask t WHERE t.id = :id")
    Optional<TaxRecordTask> findWithFileContextById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"assignedTo", "category", "subCategory", "taskName"})
    List<TaxRecordTask> findByClientIdOrderByDeadlineAscIdAsc(UUID clientId, Limit limit);

    @EntityGraph(attributePaths = {"assignedTo", "category", "subCategory", "taskName"})
    @Query("SELECT t FROM TaxRecordTask t WHERE t.client.id = :clientId " +
            "AND (t.deadline > :cursorDeadline OR (t.deadline = :cursorDeadline AND t.id > :cursorId)) " +
            "ORDER BY t.deadline ASC, t.id ASC")
    List<TaxRecordTask> findByClientIdWithCursor(
            @Param("clientId") UUID clientId,
            @Param("cursorDeadline") Instant cursorDeadline,
            @Param("cursorId") UUID cursorId,
            Limit limit);

    @EntityGraph(attributePaths = {"assignedTo", "category", "subCategory", "taskName"})
    @Query("SELECT t FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId " +
            "ORDER BY t.deadline ASC, t.id ASC")
    List<TaxRecordTask> findByClientIdAndAssignedToOrderByDeadlineAscIdAsc(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            Limit limit);

    @EntityGraph(attributePaths = {"assignedTo", "category", "subCategory", "taskName"})
    @Query("SELECT t FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId " +
            "AND (t.deadline > :cursorDeadline OR (t.deadline = :cursorDeadline AND t.id > :cursorId)) " +
            "ORDER BY t.deadline ASC, t.id ASC")
    List<TaxRecordTask> findByClientIdAndAssignedToWithCursor(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("cursorDeadline") Instant cursorDeadline,
            @Param("cursorId") UUID cursorId,
            Limit limit);

    long countByClientId(UUID clientId);

    long countByClientIdAndStatus(UUID clientId, TaxRecordTaskStatus status);

    long countByClientIdAndStatusNot(UUID clientId, TaxRecordTaskStatus status);

    long countByClientIdAndStatusNotAndDeadlineBefore(UUID clientId, TaxRecordTaskStatus status, Instant deadline);

    long countByClientIdAndStatusAndDeadlineBefore(UUID clientId, TaxRecordTaskStatus status, Instant deadline);

    @Query("SELECT MIN(t.deadline) FROM TaxRecordTask t WHERE t.client.id = :clientId AND t.status != :status")
    Optional<Instant> findNearestDeadlineByClientIdAndStatusNot(
            @Param("clientId") UUID clientId,
            @Param("status") TaxRecordTaskStatus status);

    @Query("SELECT MIN(t.deadline) FROM TaxRecordTask t WHERE t.client.id = :clientId AND t.status = :status")
    Optional<Instant> findNearestDeadlineByClientIdAndStatus(
            @Param("clientId") UUID clientId,
            @Param("status") TaxRecordTaskStatus status);

    @Query("SELECT COUNT(t) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId")
    long countByClientIdAndAssignedToUserId(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId);

    @Query("SELECT COUNT(t) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId AND t.status = :status")
    long countByClientIdAndAssignedToUserIdAndStatus(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status);

    @Query("SELECT COUNT(t) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId AND t.status != :status")
    long countByClientIdAndAssignedToUserIdAndStatusNot(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status);

    @Query("SELECT COUNT(t) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId AND t.status = :status AND t.deadline < :deadline")
    long countByClientIdAndAssignedToUserIdAndStatusAndDeadlineBefore(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status,
            @Param("deadline") Instant deadline);

    @Query("SELECT COUNT(t) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId AND t.status != :status AND t.deadline < :deadline")
    long countByClientIdAndAssignedToUserIdAndStatusNotAndDeadlineBefore(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status,
            @Param("deadline") Instant deadline);

    @Query("SELECT MIN(t.deadline) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId AND t.status = :status")
    Optional<Instant> findNearestDeadlineByClientIdAndAssignedToUserIdAndStatus(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status);

    @Query("SELECT MIN(t.deadline) FROM TaxRecordTask t JOIN t.assignedTo a WHERE t.client.id = :clientId AND a.id = :userId AND t.status != :status")
    Optional<Instant> findNearestDeadlineByClientIdAndAssignedToUserIdAndStatusNot(
            @Param("clientId") UUID clientId,
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status);

    // --- Dashboard stats (single query, scoped to assigned user) ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FILTER (WHERE t.status = 'OPEN') AS openTasks,
                   COUNT(*) FILTER (WHERE t.status = 'OPEN' AND t.created_at >= :todayStart) AS newToday,
                   COUNT(*) FILTER (WHERE t.status = 'SUBMITTED') AS submittedTasks,
                   COUNT(*) FILTER (WHERE t.status = 'APPROVED_FOR_FILING') AS forFilingTasks
            FROM tax_record_tasks t
            JOIN tax_record_task_accountants ta ON ta.task_id = t.id
            WHERE ta.user_id = :userId
            """)
    DashboardStatsProjection findDashboardStatsByUserId(
            @Param("userId") UUID userId,
            @Param("todayStart") Instant todayStart);

    @EntityGraph(attributePaths = {"client", "category", "subCategory", "taskName", "createdBy"})
    @Query("SELECT t FROM TaxRecordTask t JOIN t.assignedTo a " +
            "WHERE a.id = :userId AND t.status IN :statuses AND t.deadline < :now " +
            "ORDER BY t.deadline ASC")
    List<TaxRecordTask> findOverdueByUserId(
            @Param("userId") UUID userId,
            @Param("statuses") List<TaxRecordTaskStatus> statuses,
            @Param("now") Instant now);

    @EntityGraph(attributePaths = {"client", "category", "subCategory", "taskName", "createdBy"})
    @Query("SELECT t FROM TaxRecordTask t JOIN t.assignedTo a " +
            "WHERE a.id = :userId AND t.status IN :status " +
            "ORDER BY t.deadline ASC")
    List<TaxRecordTask> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") List<TaxRecordTaskStatus> status);

    @EntityGraph(attributePaths = {"client", "category", "subCategory", "taskName", "createdBy"})
    @Query(value = "SELECT t FROM TaxRecordTask t JOIN t.assignedTo a " +
            "WHERE a.id = :userId AND t.status IN :status",
            countQuery = "SELECT COUNT(t) FROM TaxRecordTask t JOIN t.assignedTo a " +
            "WHERE a.id = :userId AND t.status IN :status")
    Page<TaxRecordTask> findPageByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") List<TaxRecordTaskStatus> status,
            Pageable pageable);

    // --- Reviewer (QTD) queue & decisions ---

    @EntityGraph(attributePaths = {"client", "assignedTo", "category", "subCategory", "taskName"})
    @Query("SELECT t FROM TaxRecordTask t JOIN t.client c JOIN c.accountants a " +
            "WHERE a.id = :userId AND t.status = :status")
    List<TaxRecordTask> findByReviewerClientScopeAndStatus(
            @Param("userId") UUID userId,
            @Param("status") TaxRecordTaskStatus status);

    @EntityGraph(attributePaths = {"client", "assignedTo", "category", "subCategory", "taskName"})
    @Query("SELECT t FROM TaxRecordTask t WHERE t.id IN :ids")
    List<TaxRecordTask> findWithDetailsByIdIn(@Param("ids") List<UUID> ids);

    // --- Reviewer (QTD) dashboard ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FILTER (WHERE t.status = 'SUBMITTED') AS awaitingReview,
                   COUNT(*) FILTER (WHERE t.status = 'SUBMITTED' AND EXISTS (
                       SELECT 1 FROM tax_record_task_logs sl
                       WHERE sl.task_id = t.id AND sl.action = 'SUBMITTED' AND sl.created_at >= :todayStart
                   )) AS newToday
            FROM tax_record_tasks t
            JOIN client_accountants ca ON ca.client_id = t.client_id
            WHERE ca.user_id = :userId
            """)
    ReviewerTaskStatsProjection findReviewerTaskStatsByUserId(
            @Param("userId") UUID userId,
            @Param("todayStart") Instant todayStart);

    // --- Analytics queries ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FILTER (WHERE t.status = 'OPEN') AS open,
                   COUNT(*) FILTER (WHERE t.status = 'SUBMITTED') AS submitted,
                   COUNT(*) FILTER (WHERE t.status = 'REJECTED') AS rejected,
                   COUNT(*) FILTER (WHERE t.status = 'APPROVED_FOR_FILING') AS approvedForFiling,
                   COUNT(*) FILTER (WHERE t.status = 'FILED') AS filed,
                   COUNT(*) FILTER (WHERE t.status = 'COMPLETED') AS completed,
                   COUNT(*) FILTER (WHERE t.status IN ('OPEN', 'SUBMITTED', 'REJECTED') AND t.deadline < :now) AS overdue
            FROM tax_record_tasks t
            JOIN tax_record_task_accountants ta ON ta.task_id = t.id
            WHERE ta.user_id = :userId
            """)
    TaskSummaryProjection findTaskSummaryByUserId(
            @Param("userId") UUID userId,
            @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT c.name AS category,
                   COUNT(t.id) AS total,
                   COUNT(t.id) FILTER (WHERE t.status <> 'COMPLETED') AS active,
                   COUNT(t.id) FILTER (WHERE t.status = 'COMPLETED') AS completed
            FROM tax_record_tasks t
            JOIN tax_record_task_accountants ta ON ta.task_id = t.id
            JOIN tax_task_categories c ON c.id = t.category_id
            WHERE ta.user_id = :userId
            GROUP BY c.name
            ORDER BY total DESC
            """)
    List<CategoryStatsProjection> findCategoryStatsByUserId(@Param("userId") UUID userId);

    // --- Batch metrics (replaces N+1 per-client count queries) ---

    @Query(nativeQuery = true, value = """
            SELECT t.client_id AS clientId,
                   COUNT(*) AS totalTasks,
                   COUNT(*) FILTER (WHERE t.status <> 'COMPLETED') AS pendingTasks,
                   COUNT(*) FILTER (WHERE t.status IN ('OPEN', 'SUBMITTED', 'REJECTED') AND t.deadline < :now) AS overdueTasks,
                   MIN(t.deadline) FILTER (WHERE t.status <> 'COMPLETED') AS nearestDeadline
            FROM tax_record_tasks t
            WHERE t.client_id IN (:clientIds)
            GROUP BY t.client_id
            """)
    List<ClientTaskMetrics> findTaskMetricsByClientIds(
            @Param("clientIds") List<UUID> clientIds,
            @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT t.client_id AS clientId,
                   COUNT(*) AS totalTasks,
                   COUNT(*) FILTER (WHERE t.status <> 'COMPLETED') AS pendingTasks,
                   COUNT(*) FILTER (WHERE t.status IN ('OPEN', 'SUBMITTED', 'REJECTED') AND t.deadline < :now) AS overdueTasks,
                   MIN(t.deadline) FILTER (WHERE t.status <> 'COMPLETED') AS nearestDeadline
            FROM tax_record_tasks t
            JOIN tax_record_task_accountants ta ON ta.task_id = t.id
            WHERE t.client_id IN (:clientIds) AND ta.user_id = :userId
            GROUP BY t.client_id
            """)
    List<ClientTaskMetrics> findTaskMetricsByClientIdsAndUserId(
            @Param("clientIds") List<UUID> clientIds,
            @Param("userId") UUID userId,
            @Param("now") Instant now);
}

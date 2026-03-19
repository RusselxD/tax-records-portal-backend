package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.CategoryStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemCategoryStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemTaskStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.TaskSummaryProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ClientTaskMetrics;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaxRecordTaskAnalyticsRepository {

    // --- Per-user analytics ---

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

    // --- System-wide analytics ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE status = 'OPEN') AS open,
                   COUNT(*) FILTER (WHERE status = 'SUBMITTED') AS submitted,
                   COUNT(*) FILTER (WHERE status = 'APPROVED_FOR_FILING') AS approvedForFiling,
                   COUNT(*) FILTER (WHERE status = 'FILED') AS filed,
                   COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                   COUNT(*) FILTER (WHERE status = 'REJECTED') AS rejected,
                   COUNT(*) FILTER (WHERE status IN ('OPEN', 'SUBMITTED', 'REJECTED') AND deadline < :now) AS overdue,
                   COUNT(*) FILTER (WHERE status IN ('OPEN', 'SUBMITTED', 'REJECTED') AND deadline >= :todayStart AND deadline < :tomorrowStart) AS dueToday,
                   COUNT(*) FILTER (WHERE status IN ('OPEN', 'SUBMITTED', 'REJECTED') AND deadline >= :todayStart AND deadline < :weekEnd) AS dueThisWeek,
                   COUNT(*) FILTER (WHERE created_at >= :monthStart) AS createdThisMonth
            FROM tax_record_tasks
            """)
    SystemTaskStatsProjection findSystemTaskStats(
            @Param("now") Instant now,
            @Param("todayStart") Instant todayStart,
            @Param("tomorrowStart") Instant tomorrowStart,
            @Param("weekEnd") Instant weekEnd,
            @Param("monthStart") Instant monthStart);

    @Query(nativeQuery = true, value = """
            SELECT c.name AS category,
                   COUNT(t.id) FILTER (WHERE t.status = 'OPEN') AS open,
                   COUNT(t.id) FILTER (WHERE t.status = 'SUBMITTED') AS submitted,
                   COUNT(t.id) FILTER (WHERE t.status = 'REJECTED') AS rejected,
                   COUNT(t.id) FILTER (WHERE t.status = 'APPROVED_FOR_FILING') AS approvedForFiling,
                   COUNT(t.id) FILTER (WHERE t.status = 'FILED') AS filed,
                   COUNT(t.id) FILTER (WHERE t.status = 'COMPLETED') AS completed
            FROM tax_record_tasks t
            JOIN tax_task_categories c ON c.id = t.category_id
            GROUP BY c.name
            ORDER BY COUNT(t.id) DESC
            """)
    List<SystemCategoryStatsProjection> findSystemCategoryStats();

    @Query(nativeQuery = true, value = """
            SELECT u.id AS userId,
                   COUNT(t.id) AS activeTasks
            FROM users u
            JOIN roles r ON r.id = u.role_id
            JOIN tax_record_task_accountants ta ON ta.user_id = u.id
            JOIN tax_record_tasks t ON t.id = ta.task_id AND t.status <> 'COMPLETED'
            WHERE r.key IN ('CSD', 'OOS')
            GROUP BY u.id
            ORDER BY activeTasks DESC
            LIMIT 5
            """)
    List<Object[]> findTopAccountantWorkload();

    // --- Batch metrics ---

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

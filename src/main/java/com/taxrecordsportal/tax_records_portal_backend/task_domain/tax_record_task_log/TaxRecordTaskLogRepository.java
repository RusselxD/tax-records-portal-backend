package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.OnTimeStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.QualityStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ReviewerLogStatsProjection;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaxRecordTaskLogRepository extends JpaRepository<TaxRecordTaskLog, UUID> {

    @EntityGraph(attributePaths = {"performedBy"})
    List<TaxRecordTaskLog> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    // --- Reviewer (QTD) queue & decisions ---

    @Query("SELECT l FROM TaxRecordTaskLog l WHERE l.task.id IN :taskIds AND l.action = :action ORDER BY l.createdAt DESC")
    List<TaxRecordTaskLog> findByTaskIdsAndAction(
            @Param("taskIds") List<UUID> taskIds,
            @Param("action") TaxRecordTaskLogAction action);

    @Query("SELECT l FROM TaxRecordTaskLog l WHERE l.performedBy.id = :userId AND l.action IN :actions ORDER BY l.createdAt DESC")
    List<TaxRecordTaskLog> findRecentDecisionsByUser(
            @Param("userId") UUID userId,
            @Param("actions") List<TaxRecordTaskLogAction> actions,
            Limit limit);

    // --- Reviewer (QTD) dashboard ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FILTER (WHERE l.action = 'APPROVED' AND l.created_at >= :todayStart) AS approvedToday,
                   COUNT(*) FILTER (WHERE l.action = 'APPROVED' AND l.created_at >= :monthStart) AS approvedThisMonth,
                   COUNT(*) FILTER (WHERE l.action = 'REJECTED' AND l.created_at >= :monthStart) AS rejectedThisMonth
            FROM tax_record_task_logs l
            WHERE l.performed_by = :userId
            """)
    ReviewerLogStatsProjection findReviewerLogStatsByUserId(
            @Param("userId") UUID userId,
            @Param("todayStart") Instant todayStart,
            @Param("monthStart") Instant monthStart);

    // --- Analytics queries ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FROM tax_record_task_logs
            WHERE action = 'COMPLETED' AND performed_by = :userId AND created_at >= :since
            """)
    long countCompletedByUserSince(
            @Param("userId") UUID userId,
            @Param("since") Instant since);

    @Query(nativeQuery = true, value = """
            SELECT TO_CHAR(date_trunc('month', created_at AT TIME ZONE 'Asia/Manila'), 'YYYY-MM') AS month,
                   COUNT(*) AS completed
            FROM tax_record_task_logs
            WHERE action = 'COMPLETED' AND performed_by = :userId AND created_at >= :startDate
            GROUP BY date_trunc('month', created_at AT TIME ZONE 'Asia/Manila')
            ORDER BY date_trunc('month', created_at AT TIME ZONE 'Asia/Manila')
            """)
    List<Object[]> findMonthlyCompletionsByUser(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS totalCompleted,
                   COUNT(*) FILTER (WHERE l.created_at <= t.deadline) AS completedOnTime,
                   COUNT(*) FILTER (WHERE l.created_at > t.deadline) AS completedLate
            FROM tax_record_task_logs l
            JOIN tax_record_tasks t ON t.id = l.task_id
            WHERE l.action = 'COMPLETED' AND l.performed_by = :userId
            """)
    OnTimeStatsProjection findOnTimeStatsByUser(@Param("userId") UUID userId);

    @Query(nativeQuery = true, value = """
            WITH task_metrics AS (
                SELECT t.id,
                       COUNT(rl.id) AS rejected_count
                FROM tax_record_tasks t
                JOIN tax_record_task_accountants ta ON ta.task_id = t.id AND ta.user_id = :userId
                LEFT JOIN tax_record_task_logs rl ON rl.task_id = t.id AND rl.action = 'REJECTED'
                WHERE EXISTS (
                    SELECT 1 FROM tax_record_task_logs sl
                    WHERE sl.task_id = t.id AND sl.action = 'SUBMITTED'
                )
                GROUP BY t.id
            )
            SELECT COUNT(*) AS totalSubmitted,
                   COUNT(*) FILTER (WHERE rejected_count = 0) AS firstAttemptApproved,
                   COALESCE(AVG(rejected_count::float), 0) AS avgRejectionCycles
            FROM task_metrics
            """)
    QualityStatsProjection findQualityStatsByUser(@Param("userId") UUID userId);
}

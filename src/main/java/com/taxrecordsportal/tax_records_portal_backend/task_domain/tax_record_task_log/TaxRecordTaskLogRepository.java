package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.AccountantLogStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.ApprovalRateProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.OnTimeStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.QualityStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemCombinedLogStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemLogStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemOnTimeStatsProjection;
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

    boolean existsByTaskId(UUID taskId);

    void deleteAllByTaskId(UUID taskId);

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

    // --- Combined per-accountant log stats (Cards 3, 4, 5, 6, 8) ---

    @Query(nativeQuery = true, value = """
            WITH user_tasks AS (
                SELECT t.id, t.deadline, t.created_at AS task_created_at
                FROM tax_record_tasks t
                JOIN tax_record_task_accountants ta ON ta.task_id = t.id
                WHERE ta.user_id = :userId
            ),
            all_logs AS (
                SELECT l.task_id, l.action, l.created_at, l.performed_by, ut.deadline, ut.task_created_at
                FROM tax_record_task_logs l
                JOIN user_tasks ut ON ut.id = l.task_id
            ),
            productivity AS (
                SELECT COUNT(DISTINCT task_id) FILTER (WHERE action = 'SUBMITTED' AND performed_by = :userId AND created_at >= :monthStart) AS submittedThisMonth,
                       COUNT(DISTINCT task_id) FILTER (WHERE action = 'COMPLETED' AND performed_by = :userId AND created_at >= :monthStart) AS completedThisMonth,
                       COUNT(DISTINCT task_id) FILTER (WHERE action = 'COMPLETED' AND performed_by = :userId AND created_at >= :lastMonthStart AND created_at < :monthStart) AS completedLastMonth
                FROM all_logs
            ),
            efficiency AS (
                SELECT COUNT(*) AS totalCompleted,
                       COUNT(*) FILTER (WHERE created_at <= deadline) AS completedOnTime,
                       COALESCE(AVG(EXTRACT(EPOCH FROM (created_at - task_created_at)) / 86400.0), 0) AS avgCompletionDays
                FROM all_logs
                WHERE action = 'COMPLETED' AND performed_by = :userId
            ),
            task_quality AS (
                SELECT task_id,
                       COUNT(*) FILTER (WHERE action = 'REJECTED') AS rejection_count
                FROM all_logs
                WHERE action IN ('APPROVED', 'REJECTED', 'SUBMITTED')
                GROUP BY task_id
                HAVING COUNT(*) FILTER (WHERE action = 'APPROVED') > 0
            ),
            quality AS (
                SELECT COUNT(*) AS totalApproved,
                       COUNT(*) FILTER (WHERE rejection_count = 0) AS firstAttemptApproved,
                       COALESCE(AVG(rejection_count::float) FILTER (WHERE rejection_count > 0), 0) AS avgRejectionCycles
                FROM task_quality
            ),
            first_events AS (
                SELECT task_id,
                       MIN(created_at) FILTER (WHERE action = 'CREATED') AS created_at,
                       MIN(created_at) FILTER (WHERE action = 'SUBMITTED') AS first_submitted_at
                FROM all_logs
                GROUP BY task_id
                HAVING MIN(created_at) FILTER (WHERE action = 'SUBMITTED') IS NOT NULL
                   AND MIN(created_at) FILTER (WHERE action = 'CREATED') IS NOT NULL
            ),
            responsiveness_submit AS (
                SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (first_submitted_at - created_at)) / 86400.0), 0) AS avgDaysToFirstSubmit
                FROM first_events
            ),
            ordered_events AS (
                SELECT task_id, action, created_at,
                       LEAD(created_at) OVER (PARTITION BY task_id ORDER BY created_at) AS next_at,
                       LEAD(action) OVER (PARTITION BY task_id ORDER BY created_at) AS next_action
                FROM all_logs
                WHERE action IN ('REJECTED', 'SUBMITTED')
            ),
            responsiveness_reject AS (
                SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (next_at - created_at)) / 86400.0), 0) AS avgRejectionTurnaroundDays
                FROM ordered_events
                WHERE action = 'REJECTED' AND next_action = 'SUBMITTED'
            )
            SELECT p.submittedThisMonth, p.completedThisMonth, p.completedLastMonth,
                   e.totalCompleted, e.completedOnTime, e.avgCompletionDays,
                   q.totalApproved, q.firstAttemptApproved, q.avgRejectionCycles,
                   rs.avgDaysToFirstSubmit,
                   rr.avgRejectionTurnaroundDays
            FROM productivity p, efficiency e, quality q, responsiveness_submit rs, responsiveness_reject rr
            """)
    AccountantLogStatsProjection findAccountantLogStats(
            @Param("userId") UUID userId,
            @Param("monthStart") Instant monthStart,
            @Param("lastMonthStart") Instant lastMonthStart);

    // --- System-wide analytics queries (no user scope) ---

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FILTER (WHERE action = 'COMPLETED' AND created_at >= :monthStart) AS completedThisMonth,
                   COUNT(*) FILTER (WHERE action = 'REJECTED' AND created_at >= :monthStart) AS rejectedThisMonth
            FROM tax_record_task_logs
            """)
    SystemLogStatsProjection findSystemLogStats(@Param("monthStart") Instant monthStart);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS totalCompleted,
                   COUNT(*) FILTER (WHERE l.created_at <= t.deadline) AS completedOnTime,
                   COUNT(*) FILTER (WHERE l.created_at > t.deadline) AS completedLate,
                   COALESCE(AVG(EXTRACT(EPOCH FROM (l.created_at - t.created_at)) / 86400.0), 0) AS avgCompletionDays
            FROM tax_record_task_logs l
            JOIN tax_record_tasks t ON t.id = l.task_id
            WHERE l.action = 'COMPLETED'
            """)
    SystemOnTimeStatsProjection findSystemOnTimeAndAvgStats();

    @Query(nativeQuery = true, value = """
            WITH task_metrics AS (
                SELECT t.id,
                       COUNT(rl.id) AS rejected_count
                FROM tax_record_tasks t
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
    QualityStatsProjection findSystemQualityStats();

    // Combined: merges findSystemLogStats + findSystemOnTimeAndAvgStats + findSystemQualityStats into 1 query
    @Query(nativeQuery = true, value = """
            WITH monthly_counts AS (
                SELECT COUNT(*) FILTER (WHERE action = 'COMPLETED' AND created_at >= :monthStart) AS completedThisMonth,
                       COUNT(*) FILTER (WHERE action = 'REJECTED' AND created_at >= :monthStart) AS rejectedThisMonth
                FROM tax_record_task_logs
            ),
            on_time_stats AS (
                SELECT COUNT(*) AS totalCompleted,
                       COUNT(*) FILTER (WHERE l.created_at <= t.deadline) AS completedOnTime,
                       COUNT(*) FILTER (WHERE l.created_at > t.deadline) AS completedLate,
                       COALESCE(AVG(EXTRACT(EPOCH FROM (l.created_at - t.created_at)) / 86400.0), 0) AS avgCompletionDays
                FROM tax_record_task_logs l
                JOIN tax_record_tasks t ON t.id = l.task_id
                WHERE l.action = 'COMPLETED'
            ),
            task_metrics AS (
                SELECT t.id,
                       COUNT(rl.id) AS rejected_count
                FROM tax_record_tasks t
                LEFT JOIN tax_record_task_logs rl ON rl.task_id = t.id AND rl.action = 'REJECTED'
                WHERE EXISTS (
                    SELECT 1 FROM tax_record_task_logs sl
                    WHERE sl.task_id = t.id AND sl.action = 'SUBMITTED'
                )
                GROUP BY t.id
            ),
            quality_stats AS (
                SELECT COUNT(*) AS totalSubmitted,
                       COUNT(*) FILTER (WHERE rejected_count = 0) AS firstAttemptApproved,
                       COALESCE(AVG(rejected_count::float), 0) AS avgRejectionCycles
                FROM task_metrics
            )
            SELECT mc.completedThisMonth, mc.rejectedThisMonth,
                   ot.totalCompleted, ot.completedOnTime, ot.completedLate, ot.avgCompletionDays,
                   qs.totalSubmitted, qs.firstAttemptApproved, qs.avgRejectionCycles
            FROM monthly_counts mc, on_time_stats ot, quality_stats qs
            """)
    SystemCombinedLogStatsProjection findSystemCombinedLogStats(@Param("monthStart") Instant monthStart);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FILTER (WHERE action = 'APPROVED') AS approved,
                   COUNT(*) FILTER (WHERE action = 'REJECTED') AS rejected
            FROM tax_record_task_logs
            WHERE created_at >= :since
            """)
    ApprovalRateProjection findApprovalRateSince(@Param("since") Instant since);

    @Query(nativeQuery = true, value = """
            SELECT TO_CHAR(date_trunc('day', created_at AT TIME ZONE 'Asia/Manila'), 'YYYY-MM-DD') AS period,
                   COUNT(*) AS cnt
            FROM tax_record_task_logs
            WHERE action = 'COMPLETED' AND created_at >= :since
            GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Manila')
            ORDER BY date_trunc('day', created_at AT TIME ZONE 'Asia/Manila')
            """)
    List<Object[]> findDailyCompletionsSince(@Param("since") Instant since);

    @Query(nativeQuery = true, value = """
            SELECT TO_CHAR(date_trunc('week', created_at AT TIME ZONE 'Asia/Manila'), 'YYYY-MM-DD') AS period,
                   COUNT(*) AS cnt
            FROM tax_record_task_logs
            WHERE action = 'COMPLETED' AND created_at >= :since
            GROUP BY date_trunc('week', created_at AT TIME ZONE 'Asia/Manila')
            ORDER BY date_trunc('week', created_at AT TIME ZONE 'Asia/Manila')
            """)
    List<Object[]> findWeeklyCompletionsSince(@Param("since") Instant since);
}

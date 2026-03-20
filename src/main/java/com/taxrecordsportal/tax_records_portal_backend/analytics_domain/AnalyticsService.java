package com.taxrecordsportal.tax_records_portal_backend.analytics_domain;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant.*;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.system.*;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.*;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.SystemCombinedLogStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.mapper.ClientMapper;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.common.util.DateUtil;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ClientTaskMetrics;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log.TaxRecordTaskLogRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TaxRecordTaskRepository taskRepository;
    private final TaxRecordTaskLogRepository logRepository;
    private final ClientRepository clientRepository;
    private final ClientInfoTaskRepository clientInfoTaskRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    public void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public TaskSummaryResponse getTaskSummary(UUID userId) {
        Instant now = Instant.now();
        TaskSummaryProjection summary = taskRepository.findTaskSummaryByUserId(userId, now);

        LocalDate thisMonthStart = LocalDate.now(DateUtil.ZONE_PH).withDayOfMonth(1);
        Instant monthStart = DateUtil.toStartOfDay(thisMonthStart);
        long completedThisMonth = logRepository.countCompletedByUserSince(userId, monthStart);

        return new TaskSummaryResponse(
                (int) summary.getOpen(),
                (int) summary.getSubmitted(),
                (int) summary.getRejected(),
                (int) summary.getApprovedForFiling(),
                (int) summary.getFiled(),
                (int) summary.getCompleted(),
                (int) summary.getOverdue(),
                (int) completedThisMonth
        );
    }

    public MonthlyThroughputResponse getMonthlyThroughput(UUID userId, int months) {
        LocalDate startMonth = LocalDate.now(DateUtil.ZONE_PH).withDayOfMonth(1).minusMonths(months - 1L);
        Instant startDate = DateUtil.toStartOfDay(startMonth);

        List<Object[]> rows = logRepository.findMonthlyCompletionsByUser(userId, startDate);
        Map<String, Integer> dataMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> ((Number) r[1]).intValue()
                ));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<MonthlyThroughputItem> data = new ArrayList<>();
        LocalDate current = startMonth;
        LocalDate endMonth = LocalDate.now(DateUtil.ZONE_PH).withDayOfMonth(1);

        while (!current.isAfter(endMonth)) {
            String key = current.format(fmt);
            data.add(new MonthlyThroughputItem(key, dataMap.getOrDefault(key, 0)));
            current = current.plusMonths(1);
        }

        return new MonthlyThroughputResponse(data);
    }

    public OnTimeRateResponse getOnTimeRate(UUID userId) {
        OnTimeStatsProjection stats = logRepository.findOnTimeStatsByUser(userId);
        int total = (int) stats.getTotalCompleted();
        int onTime = (int) stats.getCompletedOnTime();
        int late = (int) stats.getCompletedLate();
        double rate = total > 0 ? (double) onTime / total : 0.0;
        return new OnTimeRateResponse(total, onTime, late, rate);
    }

    public QualityMetricsResponse getQualityMetrics(UUID userId) {
        QualityStatsProjection stats = logRepository.findQualityStatsByUser(userId);
        int total = (int) stats.getTotalSubmitted();
        int firstAttempt = (int) stats.getFirstAttemptApproved();
        double approvalRate = total > 0 ? (double) firstAttempt / total : 0.0;
        double avgCycles = stats.getAvgRejectionCycles() != null ? stats.getAvgRejectionCycles() : 0.0;
        return new QualityMetricsResponse(total, firstAttempt, approvalRate, avgCycles);
    }

    public TasksByCategoryResponse getTasksByCategory(UUID userId) {
        List<CategoryStatsProjection> stats = taskRepository.findCategoryStatsByUserId(userId);
        List<CategoryCountItem> data = stats.stream()
                .map(s -> new CategoryCountItem(
                        s.getCategory(),
                        (int) s.getTotal(),
                        (int) s.getActive(),
                        (int) s.getCompleted()))
                .toList();
        return new TasksByCategoryResponse(data);
    }

    public PageResponse<ClientPortfolioItem> getClientPortfolio(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Client> clientPage = clientRepository.findPageByAccountantsId(userId, pageable);

        List<UUID> clientIds = clientPage.getContent().stream().map(Client::getId).toList();

        Map<UUID, ClientTaskMetrics> metricsMap = Map.of();
        if (!clientIds.isEmpty()) {
            Instant now = Instant.now();
            List<ClientTaskMetrics> metrics = taskRepository.findTaskMetricsByClientIdsAndUserId(clientIds, userId, now);
            metricsMap = metrics.stream()
                    .collect(Collectors.toMap(ClientTaskMetrics::getClientId, m -> m));
        }

        Map<UUID, ClientTaskMetrics> finalMetricsMap = metricsMap;
        List<ClientPortfolioItem> content = clientPage.getContent().stream()
                .map(client -> toClientPortfolioItem(client, finalMetricsMap.get(client.getId())))
                .toList();

        return new PageResponse<>(content, clientPage.getNumber(), clientPage.getSize(),
                clientPage.getTotalElements(), clientPage.getTotalPages());
    }

    private ClientPortfolioItem toClientPortfolioItem(Client client, ClientTaskMetrics metrics) {
        String clientName = clientMapper.computeClientName(client);
        long totalTasks = metrics != null ? metrics.getTotalTasks() : 0;
        long pendingTasks = metrics != null ? metrics.getPendingTasks() : 0;
        long overdueTasks = metrics != null ? metrics.getOverdueTasks() : 0;
        String nearestDeadline = metrics != null && metrics.getNearestDeadline() != null
                ? metrics.getNearestDeadline().atZone(ZoneOffset.UTC).toLocalDate().toString()
                : null;
        return new ClientPortfolioItem(
                client.getId(), clientName, client.getStatus(),
                totalTasks, pendingTasks, overdueTasks, nearestDeadline);
    }

    public SystemAnalyticsResponse getSystemAnalytics() {
        LocalDate todayPH = LocalDate.now(DateUtil.ZONE_PH);
        Instant now = Instant.now();
        Instant todayStart = DateUtil.toStartOfDay(todayPH);
        Instant tomorrowStart = DateUtil.toStartOfDay(todayPH.plusDays(1));
        Instant weekEnd = DateUtil.toStartOfDay(todayPH.plusDays(7));
        Instant monthStart = DateUtil.toStartOfDay(todayPH.withDayOfMonth(1));

        // 4 queries instead of 7: consolidated log stats via CTE
        SystemClientStatsProjection clientStats = clientRepository.findSystemClientStats();
        long clientsActivatedThisMonth = userRepository.countByRoleKeyAndCreatedAtSince(RoleKey.CLIENT, monthStart);
        SystemTaskStatsProjection taskStats = taskRepository.findSystemTaskStats(now, todayStart, tomorrowStart, weekEnd, monthStart);
        SystemProfileStatsProjection profileStats = clientInfoTaskRepository.findSystemProfileStats();

        // single CTE query replaces 3 separate log queries
        SystemCombinedLogStatsProjection logStats = logRepository.findSystemCombinedLogStats(monthStart);

        long totalCompleted = logStats.getTotalCompleted();
        double onTimeRate = totalCompleted > 0 ? (double) logStats.getCompletedOnTime() / totalCompleted : 0.0;

        long totalSubmitted = logStats.getTotalSubmitted();
        double firstAttemptRate = totalSubmitted > 0 ? (double) logStats.getFirstAttemptApproved() / totalSubmitted : 0.0;
        double avgRejectionCycles = logStats.getAvgRejectionCycles() != null ? logStats.getAvgRejectionCycles() : 0.0;
        double avgCompletionDays = logStats.getAvgCompletionDays() != null ? logStats.getAvgCompletionDays() : 0.0;

        return new SystemAnalyticsResponse(
                // Card 1
                (int) clientStats.getTotal(),
                (int) clientStats.getOnboarding(),
                (int) clientStats.getActive(),
                (int) clientStats.getOffboarding(),
                (int) clientStats.getInactive(),
                // Card 2
                (int) taskStats.getTotal(),
                (int) taskStats.getOpen(),
                (int) taskStats.getSubmitted(),
                (int) taskStats.getApprovedForFiling(),
                (int) taskStats.getFiled(),
                (int) taskStats.getCompleted(),
                (int) taskStats.getRejected(),
                // Card 3
                (int) taskStats.getOverdue(),
                (int) taskStats.getDueToday(),
                (int) taskStats.getDueThisWeek(),
                // Card 4
                (int) profileStats.getTotal(),
                (int) profileStats.getOnboarding(),
                (int) profileStats.getUpdates(),
                // Card 5
                (int) logStats.getCompletedThisMonth(),
                (int) taskStats.getCreatedThisMonth(),
                (int) logStats.getRejectedThisMonth(),
                // Card 6
                avgCompletionDays,
                onTimeRate,
                // Card 7
                avgRejectionCycles,
                firstAttemptRate,
                // Card 8
                (int) clientStats.getOnboarding(),
                (int) profileStats.getOnboarding(),
                (int) clientsActivatedThisMonth
        );
    }

    public List<TasksByCategorySystemItem> getSystemTasksByCategory() {
        return taskRepository.findSystemCategoryStats().stream()
                .map(s -> new TasksByCategorySystemItem(
                        s.getCategory(),
                        (int) s.getOpen(),
                        (int) s.getSubmitted(),
                        (int) s.getRejected(),
                        (int) s.getApprovedForFiling(),
                        (int) s.getFiled(),
                        (int) s.getCompleted()
                ))
                .toList();
    }

    public List<AccountantWorkloadItemResponse> getAccountantWorkload() {
        List<Object[]> rows = taskRepository.findTopAccountantWorkload();
        // only 5 users max — lightweight second query is negligible
        List<UUID> userIds = rows.stream()
                .map(r -> UUID.fromString(r[0].toString()))
                .toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return rows.stream()
                .map(r -> {
                    UUID userId = UUID.fromString(r[0].toString());
                    return new AccountantWorkloadItemResponse(
                            UserDisplayUtil.formatDisplayName(userMap.get(userId)),
                            ((Number) r[1]).intValue()
                    );
                })
                .toList();
    }

    public TaskApprovalRateResponse getTaskApprovalRate(String range) {
        LocalDate todayPH = LocalDate.now(DateUtil.ZONE_PH);
        Instant since = computeSinceForRange(range, todayPH);
        ApprovalRateProjection stats = logRepository.findApprovalRateSince(since);
        long total = stats.getApproved() + stats.getRejected();
        int approvedRate = total > 0 ? (int) Math.round(stats.getApproved() * 100.0 / total) : 0;
        return new TaskApprovalRateResponse(approvedRate, total > 0 ? 100 - approvedRate : 0);
    }

    private Instant computeSinceForRange(String range, LocalDate todayPH) {
        return switch (range) {
            case "7d"  -> todayPH.minusDays(6).atStartOfDay(DateUtil.ZONE_PH).toInstant();
            case "30d" -> todayPH.minusDays(29).atStartOfDay(DateUtil.ZONE_PH).toInstant();
            case "3m"  -> todayPH.with(DayOfWeek.MONDAY).minusWeeks(12).atStartOfDay(DateUtil.ZONE_PH).toInstant();
            default    -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid range. Use 7d, 30d, or 3m.");
        };
    }

    public TaskCompletionTrendResponse getTaskCompletionTrend(String range) {
        LocalDate todayPH = LocalDate.now(DateUtil.ZONE_PH);
        return switch (range) {
            case "7d"  -> buildDailyTrend(todayPH, 7);
            case "30d" -> buildDailyTrend(todayPH, 30);
            case "3m"  -> buildWeeklyTrend(todayPH);
            default    -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid range. Use 7d, 30d, or 3m.");
        };
    }

    private TaskCompletionTrendResponse buildDailyTrend(LocalDate today, int days) {
        LocalDate startDate = today.minusDays(days - 1);
        Instant since = startDate.atStartOfDay(DateUtil.ZONE_PH).toInstant();

        Map<String, Integer> countMap = logRepository.findDailyCompletionsSince(since).stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).intValue()));

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        DateTimeFormatter keyFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            labels.add(date.format(labelFmt));
            values.add(countMap.getOrDefault(date.format(keyFmt), 0));
        }
        return new TaskCompletionTrendResponse(labels, values);
    }

    private TaskCompletionTrendResponse buildWeeklyTrend(LocalDate today) {
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate startWeek = currentWeekStart.minusWeeks(12);
        Instant since = startWeek.atStartOfDay(DateUtil.ZONE_PH).toInstant();

        Map<String, Integer> countMap = logRepository.findWeeklyCompletionsSince(since).stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).intValue()));

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (LocalDate week = startWeek; !week.isAfter(currentWeekStart); week = week.plusWeeks(1)) {
            String key = week.format(DateTimeFormatter.ISO_LOCAL_DATE);
            labels.add(week.format(labelFmt));
            values.add(countMap.getOrDefault(key, 0));
        }
        return new TaskCompletionTrendResponse(labels, values);
    }

    public OnboardingPipelineResponse getOnboardingPipeline(UUID userId) {
        List<ClientStatusCountProjection> counts = clientRepository.countClientsByStatusForUser(userId);
        Map<String, Long> statusMap = counts.stream()
                .collect(Collectors.toMap(ClientStatusCountProjection::getStatus, ClientStatusCountProjection::getCount));

        int onboarding = statusMap.getOrDefault("ONBOARDING", 0L).intValue();
        int activeClient = statusMap.getOrDefault("ACTIVE_CLIENT", 0L).intValue();
        int offboarding = statusMap.getOrDefault("OFFBOARDING", 0L).intValue();
        int inactiveClient = statusMap.getOrDefault("INACTIVE_CLIENT", 0L).intValue();
        return new OnboardingPipelineResponse(onboarding, activeClient, offboarding, inactiveClient,
                onboarding + activeClient + offboarding + inactiveClient);
    }
}

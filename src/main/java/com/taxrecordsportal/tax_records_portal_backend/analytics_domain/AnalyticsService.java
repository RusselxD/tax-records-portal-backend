package com.taxrecordsportal.tax_records_portal_backend.analytics_domain;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.mapper.ClientMapper;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TaxRecordTaskRepository taskRepository;
    private final TaxRecordTaskLogRepository logRepository;
    private final ClientRepository clientRepository;
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

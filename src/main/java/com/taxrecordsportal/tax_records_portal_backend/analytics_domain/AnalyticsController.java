package com.taxrecordsportal.tax_records_portal_backend.analytics_domain;

import com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // --- me variants ---

    @GetMapping("/me/task-summary")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<TaskSummaryResponse> getMyTaskSummary() {
        return ResponseEntity.ok(analyticsService.getTaskSummary(getCurrentUser().getId()));
    }

    @GetMapping("/me/monthly-throughput")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<MonthlyThroughputResponse> getMyMonthlyThroughput(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(analyticsService.getMonthlyThroughput(getCurrentUser().getId(), months));
    }

    @GetMapping("/me/on-time-rate")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<OnTimeRateResponse> getMyOnTimeRate() {
        return ResponseEntity.ok(analyticsService.getOnTimeRate(getCurrentUser().getId()));
    }

    @GetMapping("/me/quality-metrics")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<QualityMetricsResponse> getMyQualityMetrics() {
        return ResponseEntity.ok(analyticsService.getQualityMetrics(getCurrentUser().getId()));
    }

    @GetMapping("/me/tasks-by-category")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<TasksByCategoryResponse> getMyTasksByCategory() {
        return ResponseEntity.ok(analyticsService.getTasksByCategory(getCurrentUser().getId()));
    }

    @GetMapping("/me/client-portfolio")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<PageResponse<ClientPortfolioItem>> getMyClientPortfolio(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analyticsService.getClientPortfolio(getCurrentUser().getId(), page, size));
    }

    @GetMapping("/me/onboarding-pipeline")
    @PreAuthorize("hasAuthority('client.view.own')")
    public ResponseEntity<OnboardingPipelineResponse> getMyOnboardingPipeline() {
        return ResponseEntity.ok(analyticsService.getOnboardingPipeline(getCurrentUser().getId()));
    }

    // --- users/{userId} variants (Manager only) ---

    @GetMapping("/users/{userId}/task-summary")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<TaskSummaryResponse> getUserTaskSummary(@PathVariable UUID userId) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getTaskSummary(userId));
    }

    @GetMapping("/users/{userId}/monthly-throughput")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<MonthlyThroughputResponse> getUserMonthlyThroughput(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "6") int months) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getMonthlyThroughput(userId, months));
    }

    @GetMapping("/users/{userId}/on-time-rate")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<OnTimeRateResponse> getUserOnTimeRate(@PathVariable UUID userId) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getOnTimeRate(userId));
    }

    @GetMapping("/users/{userId}/quality-metrics")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<QualityMetricsResponse> getUserQualityMetrics(@PathVariable UUID userId) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getQualityMetrics(userId));
    }

    @GetMapping("/users/{userId}/tasks-by-category")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<TasksByCategoryResponse> getUserTasksByCategory(@PathVariable UUID userId) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getTasksByCategory(userId));
    }

    @GetMapping("/users/{userId}/client-portfolio")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<PageResponse<ClientPortfolioItem>> getUserClientPortfolio(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getClientPortfolio(userId, page, size));
    }

    @GetMapping("/users/{userId}/onboarding-pipeline")
    @PreAuthorize("hasAuthority('user.view.all')")
    public ResponseEntity<OnboardingPipelineResponse> getUserOnboardingPipeline(@PathVariable UUID userId) {
        analyticsService.validateUserExists(userId);
        return ResponseEntity.ok(analyticsService.getOnboardingPipeline(userId));
    }
}

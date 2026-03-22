package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.accountant;

public record TaskSummaryResponse(
        // Card 1 — Task Pipeline
        int open,
        int submitted,
        int rejected,
        int approvedForFiling,
        int filed,
        int completed,

        // Card 2 — Deadlines
        int overdue,
        int dueToday,
        int dueThisWeek,

        // Card 3 — Productivity
        int completedThisMonth,
        int submittedThisMonth,
        int newTasksThisMonth,

        // Card 4 — Efficiency
        double onTimeRate,
        double avgCompletionDays,

        // Card 5 — Quality
        double firstAttemptApprovalRate,
        double avgRejectionCycles,

        // Card 6 — Responsiveness
        double avgDaysToFirstSubmit,
        double avgRejectionTurnaroundDays,

        // Card 7 — Workload
        int activeTaskCount,
        int assignedClientCount,

        // Card 8 — Trend
        int completedLastMonth,
        Double percentChange
) {}

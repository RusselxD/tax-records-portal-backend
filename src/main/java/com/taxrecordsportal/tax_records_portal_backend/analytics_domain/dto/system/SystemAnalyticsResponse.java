package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.system;

public record SystemAnalyticsResponse(
        // Card 1 — Client Portfolio
        int totalClients,
        int onboardingClients,
        int activeClients,
        int offboardingClients,
        int inactiveClients,

        // Card 2 — Task Pipeline
        int totalTasks,
        int openTasks,
        int submittedTasks,
        int approvedForFilingTasks,
        int filedTasks,
        int completedTasks,
        int rejectedTasks,

        // Card 3 — Overdue & Upcoming
        int totalOverdueTasks,
        int tasksDueToday,
        int tasksDueThisWeek,

        // Card 4 — Review Queue
        int profilesPendingReview,
        int onboardingProfilesPending,
        int profileUpdatesPending,

        // Card 5 — Monthly Activity
        int tasksCompletedThisMonth,
        int tasksCreatedThisMonth,
        int tasksRejectedThisMonth,

        // Card 6 — Efficiency
        double avgTaskCompletionInDays,
        double onTimeRate,

        // Card 7 — Quality
        double avgRejectionCyclesPerTask,
        double firstAttemptApprovalRate,

        // Card 8 — Onboarding Funnel (reused values)
        int onboardingClientsCopy,
        int onboardingProfilesPendingCopy,
        int clientsActivatedThisMonth
) {}

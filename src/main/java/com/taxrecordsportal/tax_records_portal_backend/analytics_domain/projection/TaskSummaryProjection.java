package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.projection;

public interface TaskSummaryProjection {
    // Card 1 — Task Pipeline
    long getOpen();
    long getSubmitted();
    long getRejected();
    long getApprovedForFiling();
    long getFiled();
    long getCompleted();

    // Card 2 — Deadlines
    long getOverdue();
    long getDueToday();
    long getDueThisWeek();

    // Card 3 — Productivity (partial)
    long getNewTasksThisMonth();

    // Card 7 — Workload
    long getActiveTaskCount();
    long getAssignedClientCount();
}

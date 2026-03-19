package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto;

import java.time.Instant;
import java.util.UUID;

public interface ClientTaskMetrics {
    UUID getClientId();
    long getTotalTasks();
    long getPendingTasks();
    long getOverdueTasks();
    Instant getNearestDeadline();
}

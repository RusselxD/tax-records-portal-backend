package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification;

public enum NotificationType {
    TASK_ASSIGNED,
    TASK_SUBMITTED,
    TASK_APPROVED,
    TASK_REJECTED,
    TASK_FILED,
    TASK_COMPLETED,

    CLIENT_HANDOFF,
    OFFBOARDING_ASSIGNED,

    PROFILE_SUBMITTED,
    PROFILE_REJECTED,
    PROFILE_APPROVED,

    CONSULTATION_SUBMITTED,
    CONSULTATION_APPROVED,
    CONSULTATION_REJECTED
}

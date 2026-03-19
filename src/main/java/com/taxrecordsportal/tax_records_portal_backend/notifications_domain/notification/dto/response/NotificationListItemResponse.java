package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;

import java.time.Instant;
import java.util.UUID;

public record NotificationListItemResponse(
        UUID id,
        NotificationType type,
        UUID referenceId,
        ReferenceType referenceType,
        String message,
        boolean isRead,
        Instant createdAt
) {}

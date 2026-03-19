package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification;

import com.taxrecordsportal.tax_records_portal_backend.common.dto.ScrollResponse;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.dto.response.NotificationListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.dto.response.UnreadNotificationsCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('notification.receive')")
    public ResponseEntity<ScrollResponse<NotificationListItemResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(page, size));
    }

    @GetMapping("unread-count")
    @PreAuthorize("hasAuthority('notification.receive')")
    public ResponseEntity<UnreadNotificationsCountResponse> getUnreadNotificationsCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }


    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAuthority('notification.receive')")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }
}

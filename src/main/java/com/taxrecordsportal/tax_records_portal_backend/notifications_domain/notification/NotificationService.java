package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification;

import com.taxrecordsportal.tax_records_portal_backend.common.dto.ScrollResponse;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.dto.response.NotificationListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.dto.response.UnreadNotificationsCountResponse;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.mapper.NotificationMapper;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public ScrollResponse<NotificationListItemResponse> getMyNotifications(int page, int size, Boolean unread) {
        UUID userId = getCurrentUser().getId();
        var pageable = PageRequest.of(page, size);
        var result = Boolean.TRUE.equals(unread)
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return ScrollResponse.from(result.map(notificationMapper::toListItem));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationsCountResponse getUnreadCount() {
        long count = notificationRepository.countByRecipientIdAndReadFalse(getCurrentUser().getId());
        return new UnreadNotificationsCountResponse(count);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        int updated = notificationRepository.markAsReadByIdAndRecipientId(notificationId, getCurrentUser().getId());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllReadByRecipientId(getCurrentUser().getId());
    }

    @Transactional
    public void delete(UUID notificationId) {
        int deleted = notificationRepository.deleteByIdAndRecipientId(notificationId, getCurrentUser().getId());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
    }

    @Transactional
    public void notifyAll(List<User> recipients, NotificationType type, UUID referenceId,
                          ReferenceType referenceType, String message) {
        List<Notification> notifications = recipients.stream()
                .map(recipient -> {
                    Notification n = new Notification();
                    n.setRecipient(recipient);
                    n.setType(type);
                    n.setReferenceId(referenceId);
                    n.setReferenceType(referenceType);
                    n.setMessage(message);
                    return n;
                })
                .toList();
        notificationRepository.saveAll(notifications);
    }
}

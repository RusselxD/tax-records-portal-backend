package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewerNotificationHelper {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Notifies all active managers + active QTD accountants assigned to the client.
     */
    public void notifyReviewers(Client client, UUID referenceId, NotificationType type,
                                ReferenceType referenceType, String message) {
        List<User> managers = userRepository.findByRole_KeyInAndStatus(
                List.of(RoleKey.MANAGER), UserStatus.ACTIVE);

        Set<User> accountants = client.getAccountants();
        List<User> assignedQtd = accountants != null
                ? accountants.stream()
                    .filter(u -> u.getRole().getKey() == RoleKey.QTD && u.getStatus() == UserStatus.ACTIVE)
                    .toList()
                : List.of();

        List<User> reviewers = new ArrayList<>(managers);
        reviewers.addAll(assignedQtd);

        if (!reviewers.isEmpty()) {
            notificationService.notifyAll(reviewers, type, referenceId, referenceType, message);
        }
    }
}

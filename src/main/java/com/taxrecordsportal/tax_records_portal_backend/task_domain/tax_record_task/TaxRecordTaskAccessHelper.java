package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Component
@RequiredArgsConstructor
public class TaxRecordTaskAccessHelper {

    private final TaxRecordTaskRepository taxRecordTaskRepository;

    public TaxRecordTask findTaskOrThrow(UUID id) {
        return taxRecordTaskRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    public TaxRecordTask findTaskForFileOps(UUID id) {
        return taxRecordTaskRepository.findWithFileContextById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    public void enforceViewAccess(TaxRecordTask task) {
        User currentUser = getCurrentUser();
        if (hasPermission(currentUser, "task.view.all")) {
            return;
        }
        if (currentUser.getRole().getKey() == RoleKey.QTD) {
            enforceClientAssignment(task, currentUser);
        } else {
            enforceAssigned(task, currentUser);
        }
    }

    public void enforceReviewAccess(TaxRecordTask task) {
        User currentUser = getCurrentUser();
        if (hasPermission(currentUser, "task.view.all")) {
            return;
        }
        if (currentUser.getRole().getKey() == RoleKey.QTD) {
            enforceClientAssignment(task, currentUser);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to review this task");
        }
    }

    private void enforceClientAssignment(TaxRecordTask task, User user) {
        boolean isAssigned = task.getClient().getAccountants() != null
                && task.getClient().getAccountants().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (!isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this client");
        }
    }

    public void enforceAssigned(TaxRecordTask task) {
        enforceAssigned(task, getCurrentUser());
    }

    public void enforceAssigned(TaxRecordTask task, User user) {
        boolean isAssigned = task.getAssignedTo() != null
                && task.getAssignedTo().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (!isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this task");
        }
    }

    public boolean hasPermission(User user, String permission) {
        return user.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), permission));
    }
}

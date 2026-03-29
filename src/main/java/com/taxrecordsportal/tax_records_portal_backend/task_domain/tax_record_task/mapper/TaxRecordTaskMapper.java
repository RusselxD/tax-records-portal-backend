package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.mapper;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.ClientInformation;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTask;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.ClientTaxRecordTaskItem;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.ReviewerDecidedItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.ReviewerQueueItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaskActionsResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskDetailResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordTaskListItemResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import static com.taxrecordsportal.tax_records_portal_backend.common.util.DateUtil.ZONE_PH;
import java.util.List;
import java.util.Set;

@Component
public class TaxRecordTaskMapper {

    private static final Set<TaxRecordTaskStatus> OVERDUE_EXCLUDED = Set.of(
            TaxRecordTaskStatus.SUBMITTED, TaxRecordTaskStatus.APPROVED_FOR_FILING, TaxRecordTaskStatus.FILED, TaxRecordTaskStatus.COMPLETED);

    public TaxRecordTaskListItemResponse toListItemResponse(TaxRecordTask task) {
        ClientInformation ci = extractClientInfo(task);
        LocalDate deadline = task.getDeadline().atZone(ZONE_PH).toLocalDate();
        boolean isOverdue = !deadline.isAfter(LocalDate.now()) && !OVERDUE_EXCLUDED.contains(task.getStatus());

        return new TaxRecordTaskListItemResponse(
                task.getId(),
                computeClientName(ci),
                task.getCategory().getName(),
                task.getSubCategory().getName(),
                task.getTaskName().getName(),
                task.getYear(),
                task.getPeriod(),
                task.getStatus(),
                deadline,
                formatAssignedNames(task),
                isOverdue,
                UserDisplayUtil.formatDisplayName(task.getCreatedBy()),
                task.getCreatedAt()
        );
    }

    public TaxRecordTaskListItemResponse toListItemResponse(
            TaxRecordTask task, String clientDisplayName, List<String> assignedNames) {
        LocalDate deadline = task.getDeadline().atZone(ZONE_PH).toLocalDate();
        boolean isOverdue = !deadline.isAfter(LocalDate.now()) && !OVERDUE_EXCLUDED.contains(task.getStatus());

        return new TaxRecordTaskListItemResponse(
                task.getId(),
                clientDisplayName,
                task.getCategory().getName(),
                task.getSubCategory().getName(),
                task.getTaskName().getName(),
                task.getYear(),
                task.getPeriod(),
                task.getStatus(),
                deadline,
                assignedNames,
                isOverdue,
                UserDisplayUtil.formatDisplayName(task.getCreatedBy()),
                task.getCreatedAt()
        );
    }

    public TaxRecordTaskDetailResponse toDetailResponse(TaxRecordTask task, TaskActionsResponse actions) {
        ClientInformation ci = extractClientInfo(task);
        LocalDate deadline = task.getDeadline().atZone(ZONE_PH).toLocalDate();

        return new TaxRecordTaskDetailResponse(
                task.getId(),
                task.getClient().getId(),
                computeClientName(ci),
                task.getCategory().getName(),
                task.getSubCategory().getName(),
                task.getTaskName().getName(),
                task.getYear(),
                task.getPeriod(),
                task.getDescription(),
                deadline,
                task.getStatus(),
                formatAssignedNames(task),
                UserDisplayUtil.formatDisplayName(task.getCreatedBy()),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                actions
        );
    }

    public ClientTaxRecordTaskItem toClientTaskItem(TaxRecordTask task, boolean includeAssigned, LocalDate today) {
        LocalDate deadline = task.getDeadline().atZone(ZONE_PH).toLocalDate();
        boolean isOverdue = !deadline.isAfter(today) && !OVERDUE_EXCLUDED.contains(task.getStatus());

        List<String> assignedTo = includeAssigned
                ? formatAssignedNames(task)
                : List.of();

        return new ClientTaxRecordTaskItem(
                task.getId(),
                task.getTaskName().getName(),
                task.getCategory().getName(),
                task.getPeriod(),
                task.getYear(),
                task.getStatus(),
                deadline,
                isOverdue,
                assignedTo);
    }

    public ReviewerQueueItemResponse toReviewerQueueItem(
            TaxRecordTask task, String clientName, Instant submittedAt) {
        LocalDate deadline = task.getDeadline().atZone(ZONE_PH).toLocalDate();
        boolean isOverdue = !deadline.isAfter(LocalDate.now()) && !OVERDUE_EXCLUDED.contains(task.getStatus());

        return new ReviewerQueueItemResponse(
                task.getId(),
                clientName,
                task.getTaskName().getName(),
                task.getCategory().getName(),
                task.getSubCategory().getName(),
                task.getYear(),
                task.getPeriod(),
                deadline,
                isOverdue,
                formatAssignedNames(task),
                submittedAt);
    }

    public ReviewerDecidedItemResponse toReviewerDecidedItem(
            TaxRecordTask task, String clientName, TaxRecordTaskStatus decision, Instant decidedAt) {
        LocalDate deadline = task.getDeadline().atZone(ZONE_PH).toLocalDate();
        boolean isOverdue = !deadline.isAfter(LocalDate.now()) && !OVERDUE_EXCLUDED.contains(task.getStatus());

        return new ReviewerDecidedItemResponse(
                task.getId(),
                clientName,
                task.getTaskName().getName(),
                task.getCategory().getName(),
                task.getSubCategory().getName(),
                task.getYear(),
                task.getPeriod(),
                deadline,
                isOverdue,
                formatAssignedNames(task),
                decision,
                decidedAt);
    }

    public String computeClientName(ClientInformation ci) {
        if (ci == null) return null;
        String registered = ci.registeredName();
        String trade = ci.tradeName();
        if (registered != null && trade != null) return registered + " (" + trade + ")";
        if (registered != null) return registered;
        return trade;
    }

    private ClientInformation extractClientInfo(TaxRecordTask task) {
        return task.getClient().getClientInfo() != null
                ? task.getClient().getClientInfo().getClientInformation()
                : null;
    }

    private List<String> formatAssignedNames(TaxRecordTask task) {
        return task.getAssignedTo() != null
                ? task.getAssignedTo().stream()
                    .map(UserDisplayUtil::formatDisplayName)
                    .toList()
                : List.of();
    }
}

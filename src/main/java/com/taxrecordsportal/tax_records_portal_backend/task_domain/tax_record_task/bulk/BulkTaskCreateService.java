package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.bulk;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTask;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.mapper.TaxRecordTaskMapper;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.BulkTaskRowRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.BulkTaskCreateResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.BulkTaskCreateResponse.BulkTaskError;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class BulkTaskCreateService {

    private static final int MAX_BULK_ROWS = 500;
    private static final Set<RoleKey> ASSIGNABLE_ROLES = Set.of(RoleKey.CSD, RoleKey.OOS);

    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TaxTaskCategoryRepository taxTaskCategoryRepository;
    private final TaxTaskSubCategoryRepository taxTaskSubCategoryRepository;
    private final TaxTaskNameRepository taxTaskNameRepository;
    private final NotificationService notificationService;
    private final TaxRecordTaskMapper taskMapper;

    /**
     * Bulk-creates tax record tasks from parsed Excel rows.
     * Partial success: valid rows are saved even if others fail.
     */
    @Transactional
    public BulkTaskCreateResponse bulkCreateTasks(List<BulkTaskRowRequest> rows) {
        if (rows.isEmpty()) {
            return new BulkTaskCreateResponse(0, 0, List.of());
        }
        if (rows.size() > MAX_BULK_ROWS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum " + MAX_BULK_ROWS + " rows allowed per batch");
        }

        User currentUser = getCurrentUser();
        List<BulkTaskError> errors = new ArrayList<>();

        // Step 1: Batch-load referenced entities
        Map<UUID, Client> clientMap = clientRepository
                .findByIdIn(rows.stream().map(BulkTaskRowRequest::clientId).distinct().toList())
                .stream().collect(Collectors.toMap(Client::getId, Function.identity()));

        Map<UUID, User> accountantMap = userRepository
                .findAllByIdsWithRole(rows.stream().map(BulkTaskRowRequest::assignedToId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        // Step 2: Per-row validation
        List<ValidatedRow> validRows = new ArrayList<>();
        boolean isQtd = currentUser.getRole().getKey() == RoleKey.QTD;

        // pre-compute assigned client IDs for QTD users — avoids streaming accountants per row
        Set<UUID> assignedClientIds = isQtd
                ? clientMap.values().stream()
                    .filter(c -> c.getAccountants() != null
                            && c.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId())))
                    .map(Client::getId)
                    .collect(Collectors.toSet())
                : Set.of();

        for (int i = 0; i < rows.size(); i++) {
            BulkTaskRowRequest row = rows.get(i);
            String error = validateRow(row, clientMap, accountantMap);
            if (error != null) {
                errors.add(new BulkTaskError(i, error));
                continue;
            }

            // QTD users can only create tasks for clients assigned to them
            if (isQtd && !assignedClientIds.contains(row.clientId())) {
                errors.add(new BulkTaskError(i, "You are not assigned to this client"));
                continue;
            }
            validRows.add(new ValidatedRow(i, row,
                    clientMap.get(row.clientId()),
                    accountantMap.get(row.assignedToId()),
                    Period.valueOf(row.period().trim().toUpperCase()),
                    LocalDate.parse(row.deadline().trim())));
        }

        if (validRows.isEmpty()) {
            return new BulkTaskCreateResponse(0, errors.size(), errors);
        }

        // Step 3: Resolve/create category hierarchy (case-insensitive)
        Map<String, TaxTaskCategory> categoryMap = taxTaskCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), Function.identity(), (a, b) -> a));

        Map<String, TaxTaskSubCategory> subCategoryMap = taxTaskSubCategoryRepository.findAllWithCategory().stream()
                .collect(Collectors.toMap(
                        sc -> sc.getCategory().getName().toLowerCase() + "::" + sc.getName().toLowerCase(),
                        Function.identity(), (a, b) -> a));

        Map<String, TaxTaskName> taskNameMap = taxTaskNameRepository.findAllWithSubCategory().stream()
                .collect(Collectors.toMap(
                        tn -> tn.getSubCategory().getName().toLowerCase() + "::" + tn.getName().toLowerCase(),
                        Function.identity(), (a, b) -> a));

        for (ValidatedRow vr : validRows) {
            BulkTaskRowRequest row = vr.row;
            String catKey = row.category().trim().toLowerCase();
            String subCatKey = catKey + "::" + row.subCategory().trim().toLowerCase();
            String taskNameKey = row.subCategory().trim().toLowerCase() + "::" + row.taskName().trim().toLowerCase();

            categoryMap.computeIfAbsent(catKey, k -> {
                TaxTaskCategory cat = new TaxTaskCategory();
                cat.setName(row.category().trim());
                return taxTaskCategoryRepository.save(cat);
            });

            subCategoryMap.computeIfAbsent(subCatKey, k -> {
                TaxTaskSubCategory sc = new TaxTaskSubCategory();
                sc.setCategory(categoryMap.get(catKey));
                sc.setName(row.subCategory().trim());
                return taxTaskSubCategoryRepository.save(sc);
            });

            taskNameMap.computeIfAbsent(taskNameKey, k -> {
                TaxTaskName tn = new TaxTaskName();
                tn.setSubCategory(subCategoryMap.get(subCatKey));
                tn.setName(row.taskName().trim());
                return taxTaskNameRepository.save(tn);
            });
        }

        // Step 4: Group rows by task identity → one task per group
        Map<String, List<ValidatedRow>> grouped = validRows.stream()
                .collect(Collectors.groupingBy(vr -> {
                    BulkTaskRowRequest r = vr.row;
                    return vr.client.getId() + "::"
                            + r.category().trim().toLowerCase() + "::"
                            + r.subCategory().trim().toLowerCase() + "::"
                            + r.taskName().trim().toLowerCase() + "::"
                            + r.year() + "::"
                            + vr.period.name() + "::"
                            + vr.deadline;
                }, LinkedHashMap::new, Collectors.toList()));

        // Step 5: Duplicate check + create tasks
        List<UUID> batchClientIds = validRows.stream().map(vr -> vr.client.getId()).distinct().toList();
        Set<String> existingKeys = new HashSet<>(taxRecordTaskRepository.findTaskKeysByClientIds(batchClientIds));

        List<TaxRecordTask> tasksToSave = new ArrayList<>();
        // Parallel list: notification context per task (same index as tasksToSave)
        List<TaskNotificationContext> notificationContexts = new ArrayList<>();

        for (List<ValidatedRow> group : grouped.values()) {
            ValidatedRow first = group.getFirst();
            BulkTaskRowRequest row = first.row;

            String dupeKey = first.client.getId() + "::"
                    + row.category().trim().toLowerCase() + "::"
                    + row.subCategory().trim().toLowerCase() + "::"
                    + row.taskName().trim().toLowerCase() + "::"
                    + row.year() + "::"
                    + first.period.name() + "::"
                    + first.deadline;

            if (existingKeys.contains(dupeKey)) {
                for (ValidatedRow vr : group) {
                    errors.add(new BulkTaskError(vr.index,
                            "Task already exists for this client/category/period"));
                }
                continue;
            }
            existingKeys.add(dupeKey);

            String catKey = row.category().trim().toLowerCase();
            String subCatKey = catKey + "::" + row.subCategory().trim().toLowerCase();
            String taskNameKey = row.subCategory().trim().toLowerCase() + "::" + row.taskName().trim().toLowerCase();

            Set<User> assignedTo = group.stream()
                    .map(vr -> vr.accountant)
                    .collect(Collectors.toSet());

            TaxRecordTask task = new TaxRecordTask();
            task.setClient(first.client);
            task.setCategory(categoryMap.get(catKey));
            task.setSubCategory(subCategoryMap.get(subCatKey));
            task.setTaskName(taskNameMap.get(taskNameKey));
            task.setYear(row.year());
            task.setPeriod(first.period);
            task.setDeadline(first.deadline.atStartOfDay().toInstant(ZoneOffset.UTC));
            task.setDescription(row.description());
            task.setStatus(TaxRecordTaskStatus.OPEN);
            task.setCreatedBy(currentUser);
            task.setAssignedTo(assignedTo);

            tasksToSave.add(task);

            String clientName = taskMapper.computeClientName(
                    first.client.getClientInfo() != null ? first.client.getClientInfo().getClientInformation() : null);
            String message = "New task assigned: " + row.taskName().trim()
                    + " for " + (clientName != null ? clientName : "Unknown");
            notificationContexts.add(new TaskNotificationContext(new ArrayList<>(assignedTo), message));
        }

        // Batch save all tasks at once
        List<TaxRecordTask> savedTasks = taxRecordTaskRepository.saveAll(tasksToSave);

        // Notify assigned accountants (requires persisted task IDs)
        for (int i = 0; i < savedTasks.size(); i++) {
            TaxRecordTask saved = savedTasks.get(i);
            TaskNotificationContext ctx = notificationContexts.get(i);
            notificationService.notifyAll(
                    ctx.recipients,
                    NotificationType.TASK_ASSIGNED,
                    saved.getId(),
                    ReferenceType.TAX_RECORD_TASK,
                    ctx.message);
        }

        return new BulkTaskCreateResponse(savedTasks.size(), errors.size(), errors);
    }

    private String validateRow(BulkTaskRowRequest row, Map<UUID, Client> clientMap, Map<UUID, User> accountantMap) {
        Client client = clientMap.get(row.clientId());
        if (client == null) return "Client not found";
        if (client.getStatus() != ClientStatus.ACTIVE_CLIENT) return "Client is not active";

        User accountant = accountantMap.get(row.assignedToId());
        if (accountant == null) return "Accountant not found";
        if (!ASSIGNABLE_ROLES.contains(accountant.getRole().getKey())) {
            return "Accountant must be CSD or OOS";
        }

        if (client.getAccountants() == null || client.getAccountants().stream()
                .noneMatch(a -> a.getId().equals(row.assignedToId()))) {
            return "Accountant is not assigned to this client";
        }

        try {
            Period.valueOf(row.period().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid period: " + row.period();
        }

        try {
            LocalDate.parse(row.deadline().trim());
        } catch (Exception e) {
            return "Invalid deadline format (expected YYYY-MM-DD): " + row.deadline();
        }

        return null;
    }

    private record ValidatedRow(int index, BulkTaskRowRequest row, Client client, User accountant,
                                Period period, LocalDate deadline) {}

    private record TaskNotificationContext(List<User> recipients, String message) {}
}

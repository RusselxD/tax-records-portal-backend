package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.TaxRecordEntryService;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.common.util.ClientDisplayNameUtil;
import com.taxrecordsportal.tax_records_portal_backend.common.util.DateUtil;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReviewerNotificationHelper;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ClientDisplayNameProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.request.CreateTaxRecordTaskRequest;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.DashboardStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ReviewerLogStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ReviewerTaskStatsProjection;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log.TaxRecordTaskLog;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log.TaxRecordTaskLogAction;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task_log.TaxRecordTaskLogRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaskActionsResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.mapper.TaxRecordTaskMapper;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class TaxRecordTaskService {

    private static final int CURSOR_PAGE_SIZE = 20;

    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final ReviewerNotificationHelper reviewerNotificationHelper;
    private final TaxRecordTaskLogRepository taxRecordTaskLogRepository;
    private final TaxRecordTaskMapper taskMapper;
    private final TaxRecordTaskAccessHelper accessHelper;
    private final TaxRecordEntryService taxRecordEntryService;
    private final com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryRepository taxTaskCategoryRepository;
    private final com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryRepository taxTaskSubCategoryRepository;
    private final com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository taxTaskNameRepository;

    // --- Queries ---

    /** Paginated, filtered task list with permission-based scoping. */
    @Transactional(readOnly = true)
    public PageResponse<TaxRecordTaskListItemResponse> getTasks(
            String search, UUID clientId,
            Period period, TaxRecordTaskStatus status,
            UUID accountantId, TaskSortField sortBy, Sort.Direction sortDirection,
            int page, int size
    ) {
        User currentUser = getCurrentUser();
        boolean canViewAll = accessHelper.hasPermission(currentUser, "task.view.all");
        RoleKey roleKey = currentUser.getRole().getKey();

        UUID scopedUserId = null;
        UUID clientScopedUserId = null;
        UUID effectiveAccountantId = canViewAll ? accountantId : null;

        if (!canViewAll) {
            if (roleKey == RoleKey.QTD) {
                clientScopedUserId = currentUser.getId();
                effectiveAccountantId = accountantId;
            } else {
                scopedUserId = currentUser.getId();
            }
        }

        Specification<TaxRecordTask> spec = TaxRecordTaskSpecification.withFilters(
                search, clientId, period, status, effectiveAccountantId, scopedUserId, clientScopedUserId);

        Pageable pageable;
        if (sortBy != null) {
            Sort.Direction dir = sortDirection != null ? sortDirection : defaultDirection(sortBy);
            spec = spec.and(TaxRecordTaskSpecification.withSort(sortBy, dir));
            pageable = PageRequest.of(page, size);
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Page<TaxRecordTask> taskPage = taxRecordTaskRepository.findAll(spec, pageable);
        List<TaxRecordTask> tasks = taskPage.getContent();

        if (tasks.isEmpty()) {
            return PageResponse.from(taskPage.map(t -> null));
        }

        // Batch-fetch client display names (JSONB extraction — no full deserialization)
        List<UUID> clientIds = tasks.stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        // Batch-fetch assigned accountant names (avoids ManyToMany cartesian)
        List<UUID> taskIds = tasks.stream().map(TaxRecordTask::getId).toList();
        Map<UUID, List<String>> assignedNamesMap = new HashMap<>();
        for (Object[] row : taxRecordTaskRepository.findAssignedUsersByTaskIds(taskIds)) {
            UUID taskId = (UUID) row[0];
            User user = (User) row[1];
            assignedNamesMap.computeIfAbsent(taskId, k -> new ArrayList<>())
                    .add(UserDisplayUtil.formatDisplayName(user));
        }

        Page<TaxRecordTaskListItemResponse> resultPage = taskPage.map(task -> {
            String clientName = clientNameMap.getOrDefault(task.getClient().getId(), null);
            List<String> assignedNames = assignedNamesMap.getOrDefault(task.getId(), List.of());
            return taskMapper.toListItemResponse(task, clientName, assignedNames);
        });

        return PageResponse.from(resultPage);
    }

    /** Single task detail with permission-based scoping. */
    @Transactional(readOnly = true)
    public TaxRecordTaskDetailResponse getTask(UUID id) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(id);
        accessHelper.enforceViewAccess(task);
        return taskMapper.toDetailResponse(task, computeActions(task));
    }

    /** Cursor-paginated tasks for a specific client. */
    @Transactional(readOnly = true)
    public ClientTaxRecordTaskPageResponse getClientTasks(UUID clientId, String cursor) {
        User currentUser = getCurrentUser();
        boolean canViewAll = accessHelper.hasPermission(currentUser, "task.view.all");
        boolean isQtd = currentUser.getRole().getKey() == RoleKey.QTD;
        if (!canViewAll && isQtd) {
            enforceClientAssignment(clientId, currentUser);
        }
        boolean canViewClientTasks = canViewAll || isQtd;

        int fetchSize = CURSOR_PAGE_SIZE + 1;
        Limit limit = Limit.of(fetchSize);
        LocalDate today = LocalDate.now();

        List<TaxRecordTask> tasks;

        if (cursor == null || cursor.isBlank()) {
            tasks = canViewClientTasks
                    ? taxRecordTaskRepository.findByClientIdOrderByDeadlineAscIdAsc(clientId, limit)
                    : taxRecordTaskRepository.findByClientIdAndAssignedToOrderByDeadlineAscIdAsc(clientId, currentUser.getId(), limit);
        } else {
            UUID cursorId = UUID.fromString(cursor);
            TaxRecordTask cursorTask = taxRecordTaskRepository.findById(cursorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor"));
            tasks = canViewClientTasks
                    ? taxRecordTaskRepository.findByClientIdWithCursor(clientId, cursorTask.getDeadline(), cursorId, limit)
                    : taxRecordTaskRepository.findByClientIdAndAssignedToWithCursor(clientId, currentUser.getId(), cursorTask.getDeadline(), cursorId, limit);
        }

        boolean hasMore = tasks.size() > CURSOR_PAGE_SIZE;
        List<TaxRecordTask> page = hasMore ? tasks.subList(0, CURSOR_PAGE_SIZE) : tasks;

        String nextCursor = hasMore ? page.getLast().getId().toString() : null;

        List<ClientTaxRecordTaskItem> items = page.stream()
                .map(t -> taskMapper.toClientTaskItem(t, canViewClientTasks, today))
                .toList();

        return new ClientTaxRecordTaskPageResponse(items, nextCursor, hasMore);
    }

    /** Dashboard stat cards for the current accountant (CSD/OOS). */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        User currentUser = getCurrentUser();
        Instant todayStart = DateUtil.toStartOfDay(LocalDate.now(DateUtil.ZONE_PH));

        DashboardStatsProjection stats = taxRecordTaskRepository.findDashboardStatsByUserId(
                currentUser.getId(), todayStart);

        return new DashboardStatsResponse(
                stats.getOpenTasks(),
                stats.getNewToday(),
                stats.getSubmittedTasks(),
                stats.getForFilingTasks());
    }

    /** Dashboard stat cards for the QTD reviewer. */
    @Transactional(readOnly = true)
    public ReviewerDashboardStatsResponse getReviewerDashboardStats() {
        User currentUser = getCurrentUser();
        LocalDate today = LocalDate.now(DateUtil.ZONE_PH);
        Instant todayStart = DateUtil.toStartOfDay(today);
        Instant monthStart = DateUtil.toStartOfDay(today.withDayOfMonth(1));

        ReviewerTaskStatsProjection taskStats = taxRecordTaskRepository
                .findReviewerTaskStatsByUserId(currentUser.getId(), todayStart);
        ReviewerLogStatsProjection logStats = taxRecordTaskLogRepository
                .findReviewerLogStatsByUserId(currentUser.getId(), todayStart, monthStart);

        long approved = logStats.getApprovedThisMonth();
        long rejected = logStats.getRejectedThisMonth();
        Double approvalRate = (approved + rejected) > 0
                ? (double) approved / (approved + rejected)
                : null;

        return new ReviewerDashboardStatsResponse(
                (int) taskStats.getAwaitingReview(),
                (int) taskStats.getNewToday(),
                (int) logStats.getApprovedToday(),
                approvalRate);
    }

    /** SUBMITTED tasks visible to the QTD reviewer, ordered by submittedAt ASC, capped at 20. */
    @Transactional(readOnly = true)
    public List<ReviewerQueueItemResponse> getReviewerQueue() {
        User currentUser = getCurrentUser();
        List<TaxRecordTask> tasks = taxRecordTaskRepository.findByReviewerClientScopeAndStatus(
                currentUser.getId(), TaxRecordTaskStatus.SUBMITTED);

        if (tasks.isEmpty()) return List.of();

        List<UUID> taskIds = tasks.stream().map(TaxRecordTask::getId).toList();

        // Batch-fetch latest SUBMITTED log per task for submittedAt
        Map<UUID, Instant> submittedAtMap = taxRecordTaskLogRepository
                .findByTaskIdsAndAction(taskIds, TaxRecordTaskLogAction.SUBMITTED)
                .stream()
                .collect(Collectors.toMap(
                        l -> l.getTask().getId(),
                        TaxRecordTaskLog::getCreatedAt,
                        (a, b) -> a)); // keep first (DESC order → most recent wins)

        // Batch-fetch client display names
        List<UUID> clientIds = tasks.stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream()
                .collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> ClientDisplayNameUtil.format(p.getRegisteredName(), p.getTradeName())));

        return tasks.stream()
                .map(t -> {
                    Instant submittedAt = submittedAtMap.get(t.getId());
                    return taskMapper.toReviewerQueueItem(
                            t, clientNameMap.get(t.getClient().getId()), submittedAt);
                })
                .sorted(Comparator.comparing(
                        ReviewerQueueItemResponse::submittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .toList();
    }

    /** Tasks the QTD reviewer recently approved or rejected, ordered by decidedAt DESC, capped at 20. */
    @Transactional(readOnly = true)
    public List<ReviewerDecidedItemResponse> getRecentlyDecided() {
        User currentUser = getCurrentUser();
        List<TaxRecordTaskLogAction> decisionActions = List.of(
                TaxRecordTaskLogAction.APPROVED, TaxRecordTaskLogAction.REJECTED);

        List<TaxRecordTaskLog> logs = taxRecordTaskLogRepository.findRecentDecisionsByUser(
                currentUser.getId(), decisionActions, Limit.of(20));

        if (logs.isEmpty()) return List.of();

        List<UUID> taskIds = logs.stream().map(l -> l.getTask().getId()).distinct().toList();
        Map<UUID, TaxRecordTask> taskMap = taxRecordTaskRepository.findWithDetailsByIdIn(taskIds)
                .stream()
                .collect(Collectors.toMap(TaxRecordTask::getId, t -> t));

        List<UUID> clientIds = taskMap.values().stream()
                .map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream()
                .collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> ClientDisplayNameUtil.format(p.getRegisteredName(), p.getTradeName())));

        return logs.stream()
                .map(log -> {
                    TaxRecordTask task = taskMap.get(log.getTask().getId());
                    if (task == null) return null;
                    TaxRecordTaskStatus decision = log.getAction() == TaxRecordTaskLogAction.APPROVED
                            ? TaxRecordTaskStatus.APPROVED_FOR_FILING
                            : TaxRecordTaskStatus.REJECTED;
                    return taskMapper.toReviewerDecidedItem(
                            task, clientNameMap.get(task.getClient().getId()), decision, log.getCreatedAt());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /** Overdue tasks for the current accountant (OPEN/REJECTED past deadline). */
    @Transactional(readOnly = true)
    public List<TaxRecordTaskOverdueItemResponse> getMyOverdueTasks() {
        User currentUser = getCurrentUser();
        Instant now = Instant.now();

        List<TaxRecordTask> tasks = taxRecordTaskRepository.findOverdueByUserId(
                currentUser.getId(),
                List.of(TaxRecordTaskStatus.OPEN, TaxRecordTaskStatus.REJECTED),
                now);

        if (tasks.isEmpty()) return List.of();

        List<UUID> clientIds = tasks.stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        return tasks.stream().map(t -> new TaxRecordTaskOverdueItemResponse(
                t.getId(),
                clientNameMap.getOrDefault(t.getClient().getId(), null),
                t.getTaskName().getName(),
                t.getCategory().getName(),
                t.getSubCategory().getName(),
                t.getPeriod(),
                t.getYear(),
                t.getStatus(),
                t.getDeadline().atZone(DateUtil.ZONE_PH).toLocalDate(),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt()
        )).toList();
    }

    /** Rejected tasks for the current accountant. */
    @Transactional(readOnly = true)
    public List<TaxRecordTaskRejectedItemResponse> getMyRejectedTasks() {
        User user = getCurrentUser();
        Instant now = Instant.now();

        List<TaxRecordTask> tasks = taxRecordTaskRepository.findByUserIdAndStatus(
                user.getId(), List.of(TaxRecordTaskStatus.REJECTED));

        if (tasks.isEmpty()) return List.of();

        List<UUID> clientIds = tasks.stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        return tasks.stream().map(t -> new TaxRecordTaskRejectedItemResponse(
                t.getId(),
                clientNameMap.getOrDefault(t.getClient().getId(), null),
                t.getTaskName().getName(),
                t.getCategory().getName(),
                t.getSubCategory().getName(),
                t.getPeriod(),
                t.getYear(),
                t.getDeadline().atZone(DateUtil.ZONE_PH).toLocalDate(),
                t.getDeadline().isBefore(now),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt()
        )).toList();
    }

    /** Paginated overdue tasks for the current accountant. */
    @Transactional(readOnly = true)
    public PageResponse<TaxRecordTaskOverdueItemResponse> getMyOverdueTasksPaged(int page, int size) {
        User currentUser = getCurrentUser();
        Instant now = Instant.now();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deadline"));
        Page<TaxRecordTask> taskPage = taxRecordTaskRepository.findPageOverdueByUserId(
                currentUser.getId(),
                List.of(TaxRecordTaskStatus.OPEN, TaxRecordTaskStatus.REJECTED),
                now, pageable);

        if (taskPage.isEmpty()) return PageResponse.from(taskPage.map(t -> null));

        List<UUID> clientIds = taskPage.getContent().stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        return PageResponse.from(taskPage.map(t -> new TaxRecordTaskOverdueItemResponse(
                t.getId(),
                clientNameMap.getOrDefault(t.getClient().getId(), null),
                t.getTaskName().getName(),
                t.getCategory().getName(),
                t.getSubCategory().getName(),
                t.getPeriod(),
                t.getYear(),
                t.getStatus(),
                t.getDeadline().atZone(DateUtil.ZONE_PH).toLocalDate(),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt()
        )));
    }

    /** Paginated rejected tasks for the current accountant. */
    @Transactional(readOnly = true)
    public PageResponse<TaxRecordTaskRejectedItemResponse> getMyRejectedTasksPaged(int page, int size) {
        User user = getCurrentUser();
        Instant now = Instant.now();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deadline"));
        Page<TaxRecordTask> taskPage = taxRecordTaskRepository.findPageByUserIdAndStatuses(
                user.getId(), List.of(TaxRecordTaskStatus.REJECTED), pageable);

        if (taskPage.isEmpty()) return PageResponse.from(taskPage.map(t -> null));

        List<UUID> clientIds = taskPage.getContent().stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        return PageResponse.from(taskPage.map(t -> new TaxRecordTaskRejectedItemResponse(
                t.getId(),
                clientNameMap.getOrDefault(t.getClient().getId(), null),
                t.getTaskName().getName(),
                t.getCategory().getName(),
                t.getSubCategory().getName(),
                t.getPeriod(),
                t.getYear(),
                t.getDeadline().atZone(DateUtil.ZONE_PH).toLocalDate(),
                t.getDeadline().isBefore(now),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt()
        )));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaxRecordTaskTodoListItemResponse> getMyTodoTasks(int page, int size) {
        User user = getCurrentUser();
        Instant now = Instant.now();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deadline"));
        Page<TaxRecordTask> taskPage = taxRecordTaskRepository.findPageByUserIdAndStatus(
                user.getId(), List.of(TaxRecordTaskStatus.OPEN, TaxRecordTaskStatus.REJECTED), pageable);

        List<TaxRecordTask> tasks = taskPage.getContent();

        if (tasks.isEmpty()) {
            return PageResponse.from(taskPage.map(t -> null));
        }

        List<UUID> clientIds = tasks.stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        Page<TaxRecordTaskTodoListItemResponse> resultPage = taskPage.map(t -> new TaxRecordTaskTodoListItemResponse(
                t.getId(),
                clientNameMap.getOrDefault(t.getClient().getId(), null),
                t.getCategory().getName(),
                t.getSubCategory().getName(),
                t.getTaskName().getName(),
                t.getYear(),
                t.getPeriod(),
                t.getStatus(),
                t.getDeadline().atZone(DateUtil.ZONE_PH).toLocalDate(),
                t.getDeadline().isBefore(now),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt()
        ));

        return PageResponse.from(resultPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaxRecordTaskProgressListItemResponse> getMyProgressTasks(
            TaxRecordTaskStatus status, int page, int size) {
        User user = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "deadline"));
        Page<TaxRecordTask> taskPage = taxRecordTaskRepository.findPageByUserIdAndStatus(
                user.getId(), List.of(status), pageable);

        List<TaxRecordTask> tasks = taskPage.getContent();

        if (tasks.isEmpty()) {
            return PageResponse.from(taskPage.map(t -> null));
        }

        List<UUID> clientIds = tasks.stream().map(t -> t.getClient().getId()).distinct().toList();
        Map<UUID, String> clientNameMap = taxRecordTaskRepository.findClientDisplayNamesByIds(clientIds)
                .stream().collect(Collectors.toMap(
                        ClientDisplayNameProjection::getClientId,
                        p -> computeClientDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a));

        Page<TaxRecordTaskProgressListItemResponse> resultPage = taskPage.map(t -> new TaxRecordTaskProgressListItemResponse(
                t.getId(),
                clientNameMap.getOrDefault(t.getClient().getId(), null),
                t.getCategory().getName(),
                t.getSubCategory().getName(),
                t.getTaskName().getName(),
                t.getYear(),
                t.getPeriod(),
                t.getDeadline().atZone(DateUtil.ZONE_PH).toLocalDate(),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt()
        ));

        return PageResponse.from(resultPage);
    }

    // --- Single task creation ---

    @Transactional
    public CreateTaxRecordTaskResponse createTask(CreateTaxRecordTaskRequest request) {
        User currentUser = getCurrentUser();

        Client client = clientRepository.findWithAccountantsById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (client.getStatus() != com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus.ACTIVE_CLIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client is not active");
        }

        User accountant = userRepository.findById(request.assignedToId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accountant not found"));

        RoleKey accountantRole = accountant.getRole().getKey();
        if (accountantRole != RoleKey.CSD && accountantRole != RoleKey.OOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accountant must be CSD or OOS");
        }

        boolean isAssigned = client.getAccountants() != null
                && client.getAccountants().stream().anyMatch(a -> a.getId().equals(request.assignedToId()));
        if (!isAssigned) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accountant is not assigned to this client");
        }

        var category = taxTaskCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        var subCategory = taxTaskSubCategoryRepository.findById(request.subCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub category not found"));
        var taskName = taxTaskNameRepository.findById(request.taskNameId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task name not found"));

        TaxRecordTask task = new TaxRecordTask();
        task.setClient(client);
        task.setCategory(category);
        task.setSubCategory(subCategory);
        task.setTaskName(taskName);
        task.setYear(request.year());
        task.setPeriod(request.period());
        task.setDeadline(request.deadline().atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        task.setDescription(request.description());
        task.setStatus(TaxRecordTaskStatus.OPEN);
        task.setCreatedBy(currentUser);
        task.setAssignedTo(Set.of(accountant));

        TaxRecordTask saved = taxRecordTaskRepository.save(task);

        notificationService.notifyAll(
                List.of(accountant),
                NotificationType.TASK_ASSIGNED,
                saved.getId(),
                ReferenceType.TAX_RECORD_TASK,
                "New task assigned: " + taskName.getName());

        return new CreateTaxRecordTaskResponse(saved.getId());
    }

    // --- Delete ---

    @Transactional
    public void deleteTask(UUID taskId) {
        TaxRecordTask task = taxRecordTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        User currentUser = getCurrentUser();
        boolean isManager = currentUser.getRole().getKey() == RoleKey.MANAGER;

        if (!isManager && !task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the task creator or a manager can delete this task");
        }

        if (task.getStatus() != TaxRecordTaskStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only tasks with OPEN status can be deleted");
        }

        if (taxRecordTaskLogRepository.existsByTaskId(taskId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a task that has activity logs");
        }

        if (task.getWorkingFiles() != null && !task.getWorkingFiles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a task that has working files");
        }

        if (task.getOutputFile() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a task that has an output file");
        }

        if (task.getProofOfFilingFile() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a task that has a proof of filing file");
        }

        notificationService.deleteByReference(taskId, ReferenceType.TAX_RECORD_TASK);
        taxRecordTaskRepository.delete(task);
    }

    // --- Status transitions ---

    /** CSD/OOS submits task for review. OPEN/REJECTED → SUBMITTED */
    @Transactional
    public void submit(UUID taskId, Map<String, Object> comment) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(taskId);
        accessHelper.enforceAssigned(task);
        requireStatus(task, Set.of(TaxRecordTaskStatus.OPEN, TaxRecordTaskStatus.REJECTED));

        task.setStatus(TaxRecordTaskStatus.SUBMITTED);
        taxRecordTaskRepository.save(task);
        createLog(task, TaxRecordTaskLogAction.SUBMITTED, comment);

        notifyReviewers(task, NotificationType.TASK_SUBMITTED, "Task submitted for review: " + task.getTaskName().getName());
    }

    /** Manager/QTD approves submitted task. SUBMITTED → APPROVED_FOR_FILING */
    @Transactional
    public void approve(UUID taskId, Map<String, Object> comment) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(taskId);
        accessHelper.enforceReviewAccess(task);
        requireStatus(task, Set.of(TaxRecordTaskStatus.SUBMITTED));

        task.setStatus(TaxRecordTaskStatus.APPROVED_FOR_FILING);
        taxRecordTaskRepository.save(task);
        createLog(task, TaxRecordTaskLogAction.APPROVED, comment);

        notifyAssigned(task, NotificationType.TASK_APPROVED, "Task approved for filing: " + task.getTaskName().getName());
    }

    /** Manager/QTD rejects submitted task. SUBMITTED → REJECTED */
    @Transactional
    public void reject(UUID taskId, Map<String, Object> comment) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(taskId);
        accessHelper.enforceReviewAccess(task);
        requireStatus(task, Set.of(TaxRecordTaskStatus.SUBMITTED));

        task.setStatus(TaxRecordTaskStatus.REJECTED);
        taxRecordTaskRepository.save(task);
        createLog(task, TaxRecordTaskLogAction.REJECTED, comment);

        notifyAssigned(task, NotificationType.TASK_REJECTED, "Task rejected: " + task.getTaskName().getName());
    }

    /** CSD/OOS recalls submitted task back to open. SUBMITTED → OPEN */
    @Transactional
    public void recall(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(taskId);
        accessHelper.enforceAssigned(task);
        requireStatus(task, Set.of(TaxRecordTaskStatus.SUBMITTED));

        task.setStatus(TaxRecordTaskStatus.OPEN);
        taxRecordTaskRepository.save(task);
        createLog(task, TaxRecordTaskLogAction.RECALLED, null);
    }

    /** CSD/OOS marks task as filed. APPROVED_FOR_FILING → FILED */
    @Transactional
    public void markFiled(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(taskId);
        accessHelper.enforceAssigned(task);
        requireStatus(task, Set.of(TaxRecordTaskStatus.APPROVED_FOR_FILING));

        task.setStatus(TaxRecordTaskStatus.FILED);
        taxRecordTaskRepository.save(task);
        createLog(task, TaxRecordTaskLogAction.FILED, null);
    }

    /** CSD/OOS marks task as completed. FILED → COMPLETED. Merges files into tax_record_entries. */
    @Transactional
    public void markCompleted(UUID taskId) {
        TaxRecordTask task = accessHelper.findTaskOrThrow(taskId);
        accessHelper.enforceAssigned(task);
        requireStatus(task, Set.of(TaxRecordTaskStatus.FILED));

        if (task.getProofOfFilingFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proof of filing is required to mark task as completed");
        }

        task.setStatus(TaxRecordTaskStatus.COMPLETED);
        taxRecordTaskRepository.save(task);
        createLog(task, TaxRecordTaskLogAction.COMPLETED, null);

        taxRecordEntryService.mergeFromTask(task);
    }

    // --- Helpers ---

    private void requireStatus(TaxRecordTask task, Set<TaxRecordTaskStatus> allowed) {
        if (!allowed.contains(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot perform this action on a task with status: " + task.getStatus());
        }
    }

    private void createLog(TaxRecordTask task, TaxRecordTaskLogAction action, Map<String, Object> comment) {
        TaxRecordTaskLog log = new TaxRecordTaskLog();
        log.setTask(task);
        log.setAction(action);
        log.setComment(comment);
        log.setPerformedBy(getCurrentUser());
        taxRecordTaskLogRepository.save(log);
    }

    private void notifyAssigned(TaxRecordTask task, NotificationType type, String message) {
        if (task.getAssignedTo() != null && !task.getAssignedTo().isEmpty()) {
            notificationService.notifyAll(
                    new ArrayList<>(task.getAssignedTo()), type,
                    task.getId(), ReferenceType.TAX_RECORD_TASK, message);
        }
    }

    private void notifyReviewers(TaxRecordTask task, NotificationType type, String message) {
        reviewerNotificationHelper.notifyReviewers(
                task.getClient(), task.getId(), type, ReferenceType.TAX_RECORD_TASK, message);
    }

    private Sort.Direction defaultDirection(TaskSortField sortBy) {
        return switch (sortBy) {
            case deadline, createdAt -> Sort.Direction.DESC;
            default -> Sort.Direction.ASC;
        };
    }

    private String computeClientDisplayName(String registeredName, String tradeName) {
        return ClientDisplayNameUtil.format(registeredName, tradeName);
    }

    private TaskActionsResponse computeActions(TaxRecordTask task) {
        User user = getCurrentUser();
        Set<String> perms = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean canExecute = perms.contains("task.execute");
        boolean canReview = perms.contains("task.review");
        boolean canCreate = perms.contains("task.create");

        TaxRecordTaskStatus status = task.getStatus();
        boolean isOpen = status == TaxRecordTaskStatus.OPEN;
        boolean isRejected = status == TaxRecordTaskStatus.REJECTED;
        boolean isSubmitted = status == TaxRecordTaskStatus.SUBMITTED;
        boolean isApprovedForFiling = status == TaxRecordTaskStatus.APPROVED_FOR_FILING;
        boolean isFiled = status == TaxRecordTaskStatus.FILED;

        boolean hasWorkingFiles = task.getWorkingFiles() != null && !task.getWorkingFiles().isEmpty();

        return new TaskActionsResponse(
                canExecute && (isOpen || isRejected),           // canEdit
                canExecute && hasWorkingFiles && (isOpen || isRejected), // canSubmit
                canExecute && isSubmitted,                       // canRecall
                canReview && isSubmitted,                        // canApprove
                canReview && isSubmitted,                        // canReject
                canExecute && isApprovedForFiling,               // canMarkFiled
                canExecute && isFiled,                           // canMarkCompleted
                canExecute && (isOpen || isRejected),           // canUploadWorkingFiles
                canExecute && (isOpen || isRejected),           // canUploadOutput
                canExecute && isFiled,                           // canUploadProof
                canCreate && isOpen                              // canDelete
        );
    }

    private void enforceClientAssignment(UUID clientId, User user) {
        Client client = clientRepository.findWithAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        boolean isAssigned = client.getAccountants() != null
                && client.getAccountants().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (!isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this client");
        }
    }
}

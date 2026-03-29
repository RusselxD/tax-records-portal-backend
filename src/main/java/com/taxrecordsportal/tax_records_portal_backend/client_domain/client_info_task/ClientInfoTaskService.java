package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfo;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.BirTaxComplianceDetails;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.ClientInformation;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.CorporateOfficerInformation;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request.ClientInfoTaskCommentRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.request.ProfileUpdateSubmitRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ArchiveSnapshotResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ClientInfoTaskResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileReviewListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.diff.ProfileDiffService;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.SectionDiff;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.Submitter;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.dto.response.ClientInfoTaskLogCommentResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.dto.response.ClientInfoTaskLogResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.ClientInfoTaskLog;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.ClientInfoTaskLogAction;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task_log.ClientInfoTaskLogRepository;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReviewerNotificationHelper;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.mapper.UserMapper;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.taxrecordsportal.tax_records_portal_backend.common.util.ClientAccessHelper;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ClientInfoTaskService {

    private final ClientInfoTaskRepository clientInfoTaskRepository;
    private final ClientInfoTaskLogRepository clientInfoTaskLogRepository;
    private final ClientRepository clientRepository;
    private final ClientAccessHelper clientAccessHelper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ReviewerNotificationHelper reviewerNotificationHelper;
    private final ProfileDiffService profileDiffService;

    @Transactional(readOnly = true)
    public ClientInfoTaskResponse getTask(UUID taskId) {
        ClientInfoTask task = clientInfoTaskRepository.findWithFullClientDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        Client client = task.getClient();
        ClientInfo info = client.getClientInfo();

        String displayName = computeClientName(info != null ? info.getClientInformation() : null);

        String taxpayerClassification = null;
        if (info != null && info.getClientInformation() != null
                && info.getClientInformation().birTaxCompliance() != null
                && info.getClientInformation().birTaxCompliance().taxpayerClassification() != null) {
            taxpayerClassification = info.getClientInformation().birTaxCompliance().taxpayerClassification().getDisplayName();
        }

        Set<User> accountants = client.getAccountants();
        Map<Boolean, List<AccountantListItemResponse>> partitioned = (accountants == null ? Stream.<User>empty() : accountants.stream())
                .map(userMapper::toAccountantListItemResponse)
                .collect(Collectors.partitioningBy(a -> a.roleKey() == RoleKey.QTD));

        Optional<ClientInfoTask> latestTask = clientInfoTaskRepository
                .findTopByClientIdOrderBySubmittedAtDesc(client.getId());
        boolean hasActiveTask = latestTask.map(t -> t.getStatus() == ClientInfoTaskStatus.SUBMITTED).orElse(false);
        UUID activeTaskId = hasActiveTask ? latestTask.get().getId() : null;
        ClientInfoTaskType activeTaskType = hasActiveTask ? latestTask.get().getType() : null;
        ClientInfoTaskStatus lastReviewStatus = latestTask.map(ClientInfoTask::getStatus).orElse(null);

        String pocEmail = client.getUsers() != null && !client.getUsers().isEmpty()
                ? client.getUsers().iterator().next().getEmail() : null;
        if (pocEmail == null && info != null) {
            CorporateOfficerInformation coi = info.getCorporateOfficerInformation();
            if (coi != null && coi.pointOfContact() != null) {
                pocEmail = coi.pointOfContact().emailAddress();
            }
        }

        boolean isProfileApproved = latestTask
                .map(t -> t.getStatus() == ClientInfoTaskStatus.APPROVED)
                .orElse(false);

        return new ClientInfoTaskResponse(
                client.getId(),
                displayName,
                taxpayerClassification,
                client.getStatus(),
                hasActiveTask,
                activeTaskId,
                activeTaskType,
                lastReviewStatus,
                partitioned.get(false),
                partitioned.get(true),
                pocEmail,
                isProfileApproved,
                client.isHandedOff()
        );
    }

    @Transactional
    public void submitProfile(UUID clientId, ClientInfoTaskCommentRequest request) {
        Client client = clientRepository.findWithInfoAndAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (client.getStatus() != ClientStatus.ONBOARDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client is not in onboarding");
        }

        Set<User> accountants = client.getAccountants();
        boolean hasOosCsd = accountants != null && accountants.stream()
                .anyMatch(a -> a.getRole().getKey() == RoleKey.CSD || a.getRole().getKey() == RoleKey.OOS);
        boolean hasQtd = accountants != null && accountants.stream()
                .anyMatch(a -> a.getRole().getKey() == RoleKey.QTD);
        if (!hasOosCsd || !hasQtd) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Client must have at least one CSD/OOS accountant and one QTD accountant assigned before submitting");
        }

        ClientInfo info = client.getClientInfo();
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client info not found");
        }

        User currentUser = getCurrentUser();
        Map<String, Object> comment = request != null ? request.comment() : null;

        // One task per client — reuse existing, create if none
        Optional<ClientInfoTask> existingTask = clientInfoTaskRepository
                .findTopByClientIdOrderBySubmittedAtDesc(clientId);

        ClientInfoTask task;
        if (existingTask.isPresent()) {
            task = existingTask.get();
            if (task.getStatus() == ClientInfoTaskStatus.SUBMITTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A profile submission is already pending for this client");
            }
            snapshotClientInfo(task, info);
            task.setStatus(ClientInfoTaskStatus.SUBMITTED);
            task.setSubmittedBy(currentUser);
            task.setSubmittedAt(Instant.now());
            clientInfoTaskRepository.save(task);
            logAction(task, ClientInfoTaskLogAction.RESUBMITTED, comment, currentUser);
        } else {
            task = createNewTask(client, info, ClientInfoTaskType.ONBOARDING, currentUser);
            logAction(task, ClientInfoTaskLogAction.SUBMITTED, comment, currentUser);
        }

        String clientName = computeClientName(info.getClientInformation());
        String message = "Client profile submitted for review: " + (clientName != null ? clientName : "Unknown");

        reviewerNotificationHelper.notifyReviewers(
                client, task.getId(), NotificationType.PROFILE_SUBMITTED, ReferenceType.CLIENT_INFO, message);
    }

    @Transactional
    public void submitProfileUpdate(UUID clientId, ProfileUpdateSubmitRequest request) {
        Client client = clientRepository.findWithInfoAndAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (client.getStatus() != ClientStatus.ACTIVE_CLIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client is not active");
        }

        User currentUser = getCurrentUser();

        boolean isAssigned = client.getAccountants() != null
                && client.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
        if (!isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this client");
        }

        Map<String, Object> comment = request != null ? request.comment() : null;

        Optional<ClientInfoTask> existingTask = clientInfoTaskRepository
                .findTopByClientIdAndTypeOrderBySubmittedAtDesc(clientId, ClientInfoTaskType.PROFILE_UPDATE);

        ClientInfoTask task;
        if (existingTask.isPresent() && existingTask.get().getStatus() == ClientInfoTaskStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A profile update is already pending review for this client");
        } else if (existingTask.isPresent() && existingTask.get().getStatus() == ClientInfoTaskStatus.REJECTED) {
            task = existingTask.get();
            snapshotFromRequest(task, request);
            task.setStatus(ClientInfoTaskStatus.SUBMITTED);
            task.setSubmittedBy(currentUser);
            task.setSubmittedAt(Instant.now());
            clientInfoTaskRepository.save(task);
            logAction(task, ClientInfoTaskLogAction.RESUBMITTED, comment, currentUser);
        } else {
            task = new ClientInfoTask();
            task.setClient(client);
            task.setType(ClientInfoTaskType.PROFILE_UPDATE);
            task.setStatus(ClientInfoTaskStatus.SUBMITTED);
            snapshotFromRequest(task, request);
            task.setSubmittedBy(currentUser);
            task.setSubmittedAt(Instant.now());
            task.setCreatedBy(currentUser);
            clientInfoTaskRepository.save(task);
            logAction(task, ClientInfoTaskLogAction.SUBMITTED, comment, currentUser);
        }

        ClientInformation ci = request != null ? request.clientInformation() : null;
        String clientName = computeClientName(ci);
        String message = "Client profile update submitted for review: " + (clientName != null ? clientName : "Unknown");

        reviewerNotificationHelper.notifyReviewers(
                client, task.getId(), NotificationType.PROFILE_SUBMITTED, ReferenceType.CLIENT_INFO_EDIT, message);
    }

    @Transactional
    public void rejectProfile(UUID taskId, ClientInfoTaskCommentRequest request) {
        ClientInfoTask task = clientInfoTaskRepository.findWithSubmitterById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (task.getStatus() != ClientInfoTaskStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is not in submitted status");
        }

        User currentUser = getCurrentUser();
        verifyClientAccess(task.getClient().getId(), currentUser);

        task.setStatus(ClientInfoTaskStatus.REJECTED);
        clientInfoTaskRepository.save(task);

        Map<String, Object> comment = request != null ? request.comment() : null;
        logAction(task, ClientInfoTaskLogAction.REJECTED, comment, currentUser);

        // Notify the submitter
        User submitter = task.getSubmittedBy();
        if (submitter != null) {
            String clientName = computeClientName(task.getClientInformation());
            String message = "Client profile rejected: " + (clientName != null ? clientName : "Unknown");
            ReferenceType refType = task.getType() == ClientInfoTaskType.PROFILE_UPDATE
                    ? ReferenceType.CLIENT_INFO_EDIT : ReferenceType.CLIENT_INFO;
            notificationService.notifyAll(List.of(submitter), NotificationType.PROFILE_REJECTED,
                    task.getId(), refType, message);
        }
    }

    @Transactional
    public void approveProfile(UUID taskId, ClientInfoTaskCommentRequest request) {
        ClientInfoTask task = clientInfoTaskRepository.findWithClientInfoAndSubmitterById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (task.getStatus() != ClientInfoTaskStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is not in submitted status");
        }

        Client client = task.getClient();
        User currentUser = getCurrentUser();
        verifyClientAccess(client.getId(), currentUser);
        Map<String, Object> comment = request != null ? request.comment() : null;

        // Compute and persist full diff before overwrite
        ClientInfo info = client.getClientInfo();
        List<SectionDiff> diff = profileDiffService.computeDiff(info, task);
        task.setApprovedDiff(diff);
        task.setChangedSectionKeys(diff.stream().map(SectionDiff::sectionKey).toList());

        // Approve the task
        task.setStatus(ClientInfoTaskStatus.APPROVED);
        clientInfoTaskRepository.save(task);

        // Overwrite live ClientInfo with the approved snapshot
        overwriteClientInfo(info, task);

        clientRepository.save(client);

        logAction(task, ClientInfoTaskLogAction.APPROVED, comment, currentUser);

        // Notify the submitter
        User submitter = task.getSubmittedBy();
        if (submitter != null) {
            String clientName = computeClientName(task.getClientInformation());
            String message = "Client profile approved: " + (clientName != null ? clientName : "Unknown");
            ReferenceType refType = task.getType() == ClientInfoTaskType.PROFILE_UPDATE
                    ? ReferenceType.CLIENT_INFO_EDIT : ReferenceType.CLIENT_INFO;
            notificationService.notifyAll(List.of(submitter), NotificationType.PROFILE_APPROVED,
                    task.getId(), refType, message);
        }
    }

    @Transactional(readOnly = true)
    public ProfileUpdateReviewResponse getProfileUpdateReview(UUID taskId) {
        ClientInfoTask task = clientInfoTaskRepository.findWithClientInfoAndSubmitterById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        User currentUser = getCurrentUser();
        verifyClientAccess(task.getClient().getId(), currentUser);

        ClientInfo info = task.getClient().getClientInfo();
        String clientName = computeClientName(task.getClientInformation());

        User submitter = task.getSubmittedBy();
        Submitter submitterDto = submitter != null
                ? new Submitter(submitter.getId(), UserDisplayUtil.formatDisplayName(submitter))
                : null;

        Map<String, Object> comment = clientInfoTaskLogRepository
                .findTopByTaskIdAndActionInOrderByCreatedAtDesc(task.getId(),
                        List.of(ClientInfoTaskLogAction.SUBMITTED, ClientInfoTaskLogAction.RESUBMITTED))
                .map(ClientInfoTaskLog::getComment)
                .orElse(null);

        List<SectionDiff> sections = task.getStatus() == ClientInfoTaskStatus.APPROVED
                ? task.getApprovedDiff() != null ? task.getApprovedDiff() : List.of()
                : profileDiffService.computeDiff(info, task);

        return new ProfileUpdateReviewResponse(task.getClient().getId(), clientName, task.getStatus(), submitterDto, task.getSubmittedAt(), comment, sections);
    }

    @Transactional(readOnly = true)
    public ArchiveSnapshotResponse getArchiveSnapshot(UUID clientId) {
        ClientInfoTask task = clientInfoTaskRepository.findByClientIdAndType(clientId, ClientInfoTaskType.ONBOARDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No onboarding snapshot found for this client"));

        Client client = task.getClient();

        String clientDisplayName = computeClientName(task.getClientInformation());

        String taxpayerClassification = null;
        ClientInformation ci = task.getClientInformation();
        BirTaxComplianceDetails tc = ci != null ? ci.birTaxCompliance() : null;
        if (tc != null && tc.taxpayerClassification() != null) {
            taxpayerClassification = tc.taxpayerClassification().getDisplayName();
        }

        Set<User> accountants = client.getAccountants();
        Map<Boolean, List<AccountantListItemResponse>> partitioned = (accountants == null ? Stream.<User>empty() : accountants.stream())
                .map(userMapper::toAccountantListItemResponse)
                .collect(Collectors.partitioningBy(a -> a.roleKey() == RoleKey.QTD));

        return new ArchiveSnapshotResponse(
                clientDisplayName,
                taxpayerClassification,
                partitioned.get(false),
                partitioned.get(true),
                task.getSubmittedAt(),
                task.getMainDetails(),
                task.getClientInformation(),
                task.getCorporateOfficerInformation(),
                task.getAccessCredentials(),
                task.getScopeOfEngagement(),
                task.getProfessionalFees(),
                task.getOnboardingDetails()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileReviewListItemResponse> getProfileReviews(
            int page, int size, String search, ClientInfoTaskType type, ClientInfoTaskStatus status) {
        User currentUser = getCurrentUser();
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client.view.all"));

        UUID scopedAccountantId = hasViewAll ? null : currentUser.getId();

        Specification<ClientInfoTask> spec = ClientInfoTaskSpecification.withFilters(
                search, type, status, scopedAccountantId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<ProfileReviewListItemResponse> result = clientInfoTaskRepository.findAll(spec, pageable)
                .map(task -> new ProfileReviewListItemResponse(
                        task.getId(),
                        task.getClient().getId(),
                        computeClientName(task.getClientInformation()),
                        task.getType(),
                        task.getStatus(),
                        UserDisplayUtil.formatDisplayName(task.getSubmittedBy()),
                        task.getSubmittedAt()
                ));

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<ClientInfoTaskLogResponse> getLogsByTaskId(UUID taskId) {
        return clientInfoTaskLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(log -> new ClientInfoTaskLogResponse(
                        log.getId(),
                        UserDisplayUtil.formatDisplayName(log.getPerformedBy()),
                        log.getAction(),
                        log.getComment() != null && !log.getComment().isEmpty(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientInfoTaskLogCommentResponse getLogComment(UUID taskId, UUID logId) {
        var log = clientInfoTaskLogRepository.findById(logId)
                .filter(l -> l.getTask().getId().equals(taskId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log not found"));
        return new ClientInfoTaskLogCommentResponse(log.getId(), log.getComment());
    }

    private String computeClientName(ClientInformation ci) {
        if (ci == null) return null;
        String registered = ci.registeredName();
        String trade = ci.tradeName();
        if (registered != null && trade != null) return registered + " (" + trade + ")";
        if (registered != null) return registered;
        return trade;
    }

    private ClientInfoTask createNewTask(Client client, ClientInfo info, ClientInfoTaskType type, User currentUser) {
        ClientInfoTask task = new ClientInfoTask();
        task.setClient(client);
        task.setType(type);
        task.setStatus(ClientInfoTaskStatus.SUBMITTED);
        snapshotClientInfo(task, info);
        task.setSubmittedBy(currentUser);
        task.setSubmittedAt(Instant.now());
        task.setCreatedBy(currentUser);
        return clientInfoTaskRepository.save(task);
    }

    private void overwriteClientInfo(ClientInfo info, ClientInfoTask task) {
        info.setMainDetails(task.getMainDetails());
        info.setClientInformation(task.getClientInformation());
        info.setCorporateOfficerInformation(task.getCorporateOfficerInformation());
        info.setAccessCredentials(task.getAccessCredentials());
        info.setScopeOfEngagement(task.getScopeOfEngagement());
        info.setProfessionalFees(task.getProfessionalFees());
        info.setOnboardingDetails(task.getOnboardingDetails());
    }

    private void snapshotFromRequest(ClientInfoTask task, ProfileUpdateSubmitRequest request) {
        if (request == null) return;
        task.setMainDetails(request.mainDetails());
        task.setClientInformation(request.clientInformation());
        task.setCorporateOfficerInformation(request.corporateOfficerInformation());
        task.setAccessCredentials(request.accessCredentials());
        task.setScopeOfEngagement(request.scopeOfEngagement());
        task.setProfessionalFees(request.professionalFees());
        task.setOnboardingDetails(request.onboardingDetails());
    }

    private void snapshotClientInfo(ClientInfoTask task, ClientInfo info) {
        task.setMainDetails(info.getMainDetails());
        task.setClientInformation(info.getClientInformation());
        task.setCorporateOfficerInformation(info.getCorporateOfficerInformation());
        task.setAccessCredentials(info.getAccessCredentials());
        task.setScopeOfEngagement(info.getScopeOfEngagement());
        task.setProfessionalFees(info.getProfessionalFees());
        task.setOnboardingDetails(info.getOnboardingDetails());
    }

    private void verifyClientAccess(UUID clientId, User currentUser) {
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client.view.all"));
        if (hasViewAll) return;

        Client client = clientRepository.findWithAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        clientAccessHelper.enforceAccess(client, "You are not assigned to this client", "client.view.all");
    }

    private void logAction(ClientInfoTask task, ClientInfoTaskLogAction action, Map<String, Object> comment, User performer) {
        ClientInfoTaskLog log = new ClientInfoTaskLog();
        log.setTask(task);
        log.setAction(action);
        log.setComment(comment);
        log.setPerformedBy(performer);
        clientInfoTaskLogRepository.save(log);
    }
}

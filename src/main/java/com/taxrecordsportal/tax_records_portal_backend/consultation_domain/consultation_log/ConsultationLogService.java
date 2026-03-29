package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.mapper.ClientMapper;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.ClientConsultationConfig;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.ClientConsultationConfigRepository;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request.ConsultationLogActionRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request.ConsultationLogCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.request.ConsultationLogUpdateRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogAuditCommentResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogAuditResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogDetailResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationLogListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.dto.response.ConsultationMonthlySummaryResponse;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log.mapper.ConsultationLogMapper;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileService;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit.ConsultationLogAudit;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit.ConsultationLogAuditAction;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log_audit.ConsultationLogAuditRepository;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReviewerNotificationHelper;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.DateUtil.ZONE_PH;
import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ConsultationLogService {

    private final ConsultationLogRepository consultationLogRepository;
    private final ConsultationLogAuditRepository auditRepository;
    private final ClientConsultationConfigRepository configRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final ReviewerNotificationHelper reviewerNotificationHelper;
    private final ConsultationLogMapper mapper;
    private final ClientMapper clientMapper;
    private final FileService fileService;

    @Transactional
    public ConsultationLogDetailResponse create(ConsultationLogCreateRequest request) {
        User currentUser = getCurrentUser();
        Client client = clientRepository.findWithInfoAndAccountantsById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        ConsultationLog log = new ConsultationLog();
        log.setClient(client);
        log.setDate(request.date());
        log.setStartTime(request.startTime());
        log.setEndTime(request.endTime());
        log.setHours(computeHours(request.startTime(), request.endTime()));
        log.setPlatform(request.platform());
        log.setSubject(request.subject());
        log.setNotes(request.notes());
        log.setAttachments(request.attachments());
        log.setBillableType(request.billableType() == ConsultationBillableType.COURTESY
                ? ConsultationBillableType.COURTESY
                : ConsultationBillableType.INCLUDED);
        log.setStatus(ConsultationLogStatus.DRAFT);
        log.setCreatedBy(currentUser);

        ConsultationLog saved = consultationLogRepository.save(log);
        createAudit(saved, ConsultationLogAuditAction.CREATED, null);

        String clientName = clientMapper.computeClientName(client);
        return mapper.toDetail(saved, clientName);
    }

    @Transactional
    public ConsultationLogDetailResponse update(UUID id, ConsultationLogUpdateRequest request) {
        ConsultationLog log = findAndEnforceCreator(id);
        requireStatus(log, Set.of(ConsultationLogStatus.DRAFT, ConsultationLogStatus.REJECTED));

        if (request.date() != null) log.setDate(request.date());
        if (request.startTime() != null) log.setStartTime(request.startTime());
        if (request.endTime() != null) log.setEndTime(request.endTime());
        if (request.startTime() != null || request.endTime() != null) {
            log.setHours(computeHours(log.getStartTime(), log.getEndTime()));
        }
        if (request.platform() != null) log.setPlatform(request.platform());
        if (request.subject() != null) log.setSubject(request.subject());
        if (request.notes() != null) log.setNotes(request.notes());
        if (request.attachments() != null) log.setAttachments(request.attachments());
        if (request.billableType() != null) {
            log.setBillableType(request.billableType() == ConsultationBillableType.COURTESY
                    ? ConsultationBillableType.COURTESY
                    : ConsultationBillableType.INCLUDED);
        }

        if (log.getStatus() == ConsultationLogStatus.REJECTED) {
            log.setStatus(ConsultationLogStatus.DRAFT);
        }

        consultationLogRepository.save(log);
        String clientName = clientMapper.computeClientName(log.getClient());
        return mapper.toDetail(log, clientName);
    }

    @Transactional
    public void delete(UUID id) {
        ConsultationLog log = findAndEnforceCreator(id);
        requireStatus(log, Set.of(ConsultationLogStatus.DRAFT));

        if (log.getAttachments() != null) {
            log.getAttachments().forEach(f -> fileService.delete(f.id()));
        }

        notificationService.deleteByReference(id, ReferenceType.CONSULTATION_LOG);
        consultationLogRepository.delete(log);
    }

    @Transactional(readOnly = true)
    public ConsultationLogDetailResponse getDetail(UUID id) {
        ConsultationLog log = consultationLogRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation log not found"));
        String clientName = clientMapper.computeClientName(log.getClient());
        return mapper.toDetail(log, clientName);
    }

    @Transactional(readOnly = true)
    public List<ConsultationLogAuditResponse> getAudits(UUID consultationLogId) {
        return auditRepository.findByConsultationLogIdOrderByCreatedAtDesc(consultationLogId).stream()
                .map(mapper::toAuditListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationLogAuditCommentResponse getAuditComment(UUID consultationLogId, UUID auditId) {
        ConsultationLogAudit audit = auditRepository.findById(auditId)
                .filter(a -> a.getConsultationLog().getId().equals(consultationLogId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found"));
        return new ConsultationLogAuditCommentResponse(audit.getId(), audit.getComment());
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultationLogListItemResponse> list(
            UUID clientId, ConsultationLogStatus status, ConsultationBillableType billableType,
            LocalDate dateFrom, LocalDate dateTo, String search, UUID createdById,
            int page, int size) {

        User currentUser = getCurrentUser();
        boolean canViewAll = hasPermission(currentUser, "consultation.view.all");
        RoleKey roleKey = currentUser.getRole().getKey();

        UUID scopedUserId = null;
        UUID clientScopedUserId = null;

        if (!canViewAll) {
            if (roleKey == RoleKey.QTD) {
                clientScopedUserId = currentUser.getId();
            } else {
                scopedUserId = currentUser.getId();
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ConsultationLog> result = consultationLogRepository.findAll(
                ConsultationLogSpecification.withFilters(
                        clientId, status, billableType, dateFrom, dateTo, search,
                        createdById, scopedUserId, clientScopedUserId),
                pageable);

        return PageResponse.from(result.map(log -> {
            String clientName = clientMapper.computeClientName(log.getClient());
            return mapper.toListItem(log, clientName);
        }));
    }

    @Transactional
    public void submit(UUID id, ConsultationLogActionRequest request) {
        ConsultationLog log = findAndEnforceCreator(id);
        requireStatus(log, Set.of(ConsultationLogStatus.DRAFT, ConsultationLogStatus.REJECTED));

        log.setStatus(ConsultationLogStatus.SUBMITTED);
        consultationLogRepository.save(log);
        createAudit(log, ConsultationLogAuditAction.SUBMITTED,
                request != null ? request.comment() : null);

        String message = "Consultation log submitted for review: " + log.getSubject();
        notifyReviewers(log, NotificationType.CONSULTATION_SUBMITTED, message);
    }

    @Transactional
    public void approve(UUID id, ConsultationLogActionRequest request) {
        ConsultationLog log = consultationLogRepository.findWithFullDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation log not found"));
        requireStatus(log, Set.of(ConsultationLogStatus.SUBMITTED));
        enforceReviewAccess(log);

        log.setStatus(ConsultationLogStatus.APPROVED);

        // Auto-assign billable type for non-courtesy logs
        if (log.getBillableType() != ConsultationBillableType.COURTESY) {
            recomputeBillableType(log);
        }

        consultationLogRepository.save(log);
        createAudit(log, ConsultationLogAuditAction.APPROVED,
                request != null ? request.comment() : null);

        String message = "Consultation log approved: " + log.getSubject();
        notificationService.notifyAll(
                List.of(log.getCreatedBy()),
                NotificationType.CONSULTATION_APPROVED,
                log.getId(), ReferenceType.CONSULTATION_LOG, message);
    }

    @Transactional
    public void reject(UUID id, ConsultationLogActionRequest request) {
        ConsultationLog log = consultationLogRepository.findWithFullDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation log not found"));
        requireStatus(log, Set.of(ConsultationLogStatus.SUBMITTED));
        enforceReviewAccess(log);

        log.setStatus(ConsultationLogStatus.REJECTED);
        consultationLogRepository.save(log);
        createAudit(log, ConsultationLogAuditAction.REJECTED,
                request != null ? request.comment() : null);

        String message = "Consultation log rejected: " + log.getSubject();
        notificationService.notifyAll(
                List.of(log.getCreatedBy()),
                NotificationType.CONSULTATION_REJECTED,
                log.getId(), ReferenceType.CONSULTATION_LOG, message);
    }

    @Transactional(readOnly = true)
    public ConsultationMonthlySummaryResponse getMonthlySummary(UUID clientId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal totalHours = consultationLogRepository.sumHoursByClientAndMonth(
                clientId, start, end, ConsultationLogStatus.APPROVED);
        BigDecimal courtesyHours = consultationLogRepository.sumHoursByClientAndMonthAndBillableType(
                clientId, start, end, ConsultationLogStatus.APPROVED, ConsultationBillableType.COURTESY);

        ClientConsultationConfig config = configRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Consultation config not found for this client"));

        return mapper.toMonthlySummary(year, month, totalHours, courtesyHours,
                config.getIncludedHours(), config.getExcessRate());
    }

    @Transactional(readOnly = true)
    public ConsultationMonthlySummaryResponse getMyMonthlySummary() {
        UUID userId = getCurrentUser().getId();
        UUID clientId = clientRepository.findClientIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        LocalDate today = LocalDate.now(ZONE_PH);
        return getMonthlySummary(clientId, today.getYear(), today.getMonthValue());
    }

    // --- Private helpers ---

    private void recomputeBillableType(ConsultationLog approvedLog) {
        YearMonth ym = YearMonth.of(approvedLog.getDate().getYear(), approvedLog.getDate().getMonthValue());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<ConsultationLog> monthLogs = consultationLogRepository
                .findByClientIdAndDateBetweenAndStatusOrderByDateAscStartTimeAsc(
                        approvedLog.getClient().getId(), start, end, ConsultationLogStatus.APPROVED);

        // Include the currently being approved log (not yet saved with APPROVED status in this query)
        boolean alreadyInList = monthLogs.stream().anyMatch(l -> l.getId().equals(approvedLog.getId()));
        if (!alreadyInList) {
            monthLogs = new ArrayList<>(monthLogs);
            monthLogs.add(approvedLog);
            monthLogs.sort(Comparator.comparing(ConsultationLog::getDate)
                    .thenComparing(ConsultationLog::getStartTime));
        }

        ClientConsultationConfig config = configRepository.findByClientId(approvedLog.getClient().getId())
                .orElse(null);
        BigDecimal cap = config != null ? config.getIncludedHours() : BigDecimal.ZERO;

        BigDecimal cumulative = BigDecimal.ZERO;
        for (ConsultationLog log : monthLogs) {
            if (log.getBillableType() == ConsultationBillableType.COURTESY) continue;

            cumulative = cumulative.add(log.getHours());
            if (cumulative.compareTo(cap) <= 0) {
                log.setBillableType(ConsultationBillableType.INCLUDED);
            } else {
                log.setBillableType(ConsultationBillableType.EXCESS);
            }
        }
    }

    private BigDecimal computeHours(java.time.LocalTime start, java.time.LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private ConsultationLog findAndEnforceCreator(UUID id) {
        ConsultationLog log = consultationLogRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation log not found"));
        if (!log.getCreatedBy().getId().equals(getCurrentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own consultation logs");
        }
        return log;
    }

    private void enforceReviewAccess(ConsultationLog log) {
        User currentUser = getCurrentUser();
        RoleKey roleKey = currentUser.getRole().getKey();
        if (roleKey == RoleKey.MANAGER) return;

        if (roleKey == RoleKey.QTD) {
            Set<User> accountants = log.getClient().getAccountants();
            boolean isAssignedQtd = accountants != null &&
                    accountants.stream().anyMatch(u -> u.getId().equals(currentUser.getId()));
            if (isAssignedQtd) return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have review access to this consultation log");
    }

    private void requireStatus(ConsultationLog log, Set<ConsultationLogStatus> allowed) {
        if (!allowed.contains(log.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Action not allowed in status " + log.getStatus());
        }
    }

    private void createAudit(ConsultationLog log, ConsultationLogAuditAction action, Map<String, Object> comment) {
        ConsultationLogAudit audit = new ConsultationLogAudit();
        audit.setConsultationLog(log);
        audit.setAction(action);
        audit.setComment(comment);
        audit.setPerformedBy(getCurrentUser());
        auditRepository.save(audit);
    }

    private void notifyReviewers(ConsultationLog log, NotificationType type, String message) {
        reviewerNotificationHelper.notifyReviewers(
                log.getClient(), log.getId(), type, ReferenceType.CONSULTATION_LOG, message);
    }

    private boolean hasPermission(User user, String permission) {
        return user.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), permission));
    }
}

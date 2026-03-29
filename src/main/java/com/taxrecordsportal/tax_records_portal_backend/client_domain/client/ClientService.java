package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.taxrecordsportal.tax_records_portal_backend.common.exception.ClientInfoSectionException;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.mapper.ClientMapper;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfo;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfoRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template.ClientInfoTemplate;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template.ClientInfoTemplateRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTask;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskStatus;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTaskType;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.ClientConsultationConfig;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.ClientConsultationConfigRepository;
import com.taxrecordsportal.tax_records_portal_backend.common.util.ClientAccessHelper;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.ScrollResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.ClientTaskMetrics;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientInfoRepository clientInfoRepository;
    private final ClientInfoTemplateRepository clientInfoTemplateRepository;
    private final ClientConsultationConfigRepository consultationConfigRepository;
    private final UserRepository userRepository;
    private final ClientInfoTaskRepository clientInfoTaskRepository;
    private final FileService fileService;
    private final NotificationService notificationService;
    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final ClientAccessHelper clientAccessHelper;
    private final ClientMapper clientMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<OnboardingClientListItemResponse> getOnboardingClients(String search) {
        List<Client> clients = clientRepository.findByCreatedById(getCurrentUser().getId());
        List<UUID> clientIds = clients.stream().map(Client::getId).toList();

        Map<UUID, UUID> activeTaskIdByClientId = clientInfoTaskRepository
                .findActiveTasksByClientIds(clientIds).stream()
                .collect(Collectors.toMap(t -> t.getClient().getId(), t -> t.getId()));

        Map<UUID, UUID> lastTaskIdByClientId = clientInfoTaskRepository
                .findLatestTasksByClientIds(clientIds).stream()
                .collect(Collectors.toMap(t -> t.getClient().getId(), t -> t.getId()));

        var results = clients.stream()
                .map(client -> {
                    OnboardingClientListItemResponse base = clientMapper.toOnboardingListItem(client);
                    UUID activeTaskId = activeTaskIdByClientId.get(client.getId());
                    return new OnboardingClientListItemResponse(
                            base.id(), base.name(), base.email(), base.status(),
                            base.createdAt(), base.updatedAt(),
                            activeTaskId != null,
                            activeTaskId,
                            lastTaskIdByClientId.get(client.getId()),
                            client.isHandedOff()
                    );
                })
                .toList();

        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            results = results.stream()
                    .filter(r -> r.name() != null && r.name().toLowerCase().contains(lower))
                    .toList();
        }

        return results;
    }

    @Transactional(readOnly = true)
    public ScrollResponse<AssignedClientsListItemResponse> getMyAssignedClients(int page, int size) {
        UUID userId = getCurrentUser().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Client> clientPage = clientRepository.findPageByAccountantsId(userId, pageable);
        return ScrollResponse.from(clientPage.map(client ->
                new AssignedClientsListItemResponse(clientMapper.computeClientName(client), client.getId())));
    }

    @Transactional
    public ClientCreateResponse createClient() {
        User currentUser = getCurrentUser();

        ClientInfoTemplate template = clientInfoTemplateRepository.findFirstBy()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Client info template not found"));

        Client client = new Client();
        client.setStatus(ClientStatus.ONBOARDING);
        client.setCreatedBy(currentUser);
        Client savedClient = clientRepository.save(client);

        ClientInfo clientInfo = clientMapper.fromTemplate(template);
        clientInfo.setClient(savedClient);
        clientInfoRepository.save(clientInfo);

        ClientConsultationConfig consultationConfig = new ClientConsultationConfig();
        consultationConfig.setClient(savedClient);
        consultationConfig.setIncludedHours(BigDecimal.valueOf(2));
        consultationConfig.setExcessRate(BigDecimal.valueOf(500));
        consultationConfigRepository.save(consultationConfig);

        return new ClientCreateResponse(savedClient.getId());
    }

    @Transactional(readOnly = true)
    public ClientInfoResponse getClientInfoTemplate() {
        ClientInfoTemplate template = clientInfoTemplateRepository.findFirstBy()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Client info template not found"));

        return new ClientInfoResponse(
                null, null, null, List.of(), List.of(), false, null,
                template.getMainDetails(),
                template.getClientInformation(),
                template.getCorporateOfficerInformation(),
                template.getAccessCredentials(),
                template.getScopeOfEngagement(),
                template.getProfessionalFees(),
                template.getOnboardingDetails()
        );
    }

    @Transactional(readOnly = true)
    public ClientInfoHeaderResponse getClientInfoHeader(UUID clientId) {
        Client client = findAccessibleClientWithInfo(clientId);
        ClientInfo info = requireClientInfo(client);
        return buildClientInfoHeaderResponse(client, info);
    }

    @Transactional(readOnly = true)
    public String getClientInfoSection(UUID clientId, String sectionKey) {
        enforceClientInfoAccess(clientId);

        String json = switch (sectionKey) {
            case "mainDetails" -> clientInfoRepository.findMainDetailsByClientId(clientId);
            case "clientInformation" -> clientInfoRepository.findClientInformationByClientId(clientId);
            case "corporateOfficerInformation" -> clientInfoRepository.findCorporateOfficerInformationByClientId(clientId);
            case "accessCredentials" -> clientInfoRepository.findAccessCredentialsByClientId(clientId);
            case "scopeOfEngagement" -> clientInfoRepository.findScopeOfEngagementByClientId(clientId);
            case "professionalFees" -> clientInfoRepository.findProfessionalFeesByClientId(clientId);
            case "onboardingDetails" -> clientInfoRepository.findOnboardingDetailsByClientId(clientId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section key: " + sectionKey);
        };

        if (json == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client info not found");
        }
        return json;
    }

    private void enforceClientInfoAccess(UUID clientId) {
        Client client = clientRepository.findWithCreatorAndAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        clientAccessHelper.enforceAccess(client, "You do not have access to this client", "client_info.view.all");
    }

    private Client findAccessibleClientWithInfo(UUID clientId) {
        Client client = clientRepository.findWithInfoCreatorAndAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        clientAccessHelper.enforceAccess(client, "You do not have access to this client", "client_info.view.all");
        return client;
    }

    private ClientInfo requireClientInfo(Client client) {
        ClientInfo info = client.getClientInfo();
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client info not found");
        }
        return info;
    }

    @Transactional(readOnly = true)
    public List<ClientLookupResponse> getActiveClients() {
        return clientRepository.findByStatusAndAccountantsIsNotEmpty(ClientStatus.ACTIVE_CLIENT)
                .stream()
                .map(client -> new ClientLookupResponse(
                        client.getId(),
                        clientMapper.computeClientName(client)))
                .filter(r -> r.displayName() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientListItemResponse> getClients(String search, int page, int size) {
        User currentUser = getCurrentUser();

        boolean canViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client.view.all"));

        UUID scopedUserId = canViewAll ? null : currentUser.getId();

        Specification<Client> spec = ClientSpecification.withFilters(search, scopedUserId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Client> clientPage = clientRepository.findAll(spec, pageable);

        List<UUID> clientIds = clientPage.getContent().stream().map(Client::getId).toList();

        Map<UUID, ClientTaskMetrics> metricsMap = Map.of();
        Map<UUID, Set<User>> accountantsMap = Map.of();
        if (!clientIds.isEmpty()) {
            Instant now = Instant.now();
            boolean unscopedMetrics = canViewAll || currentUser.getRole().getKey() == RoleKey.QTD;
            List<ClientTaskMetrics> metrics = unscopedMetrics
                    ? taxRecordTaskRepository.findTaskMetricsByClientIds(clientIds, now)
                    : taxRecordTaskRepository.findTaskMetricsByClientIdsAndUserId(clientIds, scopedUserId, now);
            metricsMap = metrics.stream()
                    .collect(Collectors.toMap(ClientTaskMetrics::getClientId, m -> m));

            List<Object[]> accountantRows = clientRepository.findAccountantsByClientIds(clientIds);
            Map<UUID, Set<User>> accountantsMapTemp = new HashMap<>();
            for (Object[] row : accountantRows) {
                UUID cid = (UUID) row[0];
                User accountant = (User) row[1];
                accountantsMapTemp.computeIfAbsent(cid, k -> new HashSet<>()).add(accountant);
            }
            accountantsMap = accountantsMapTemp;
        }

        Map<UUID, ClientTaskMetrics> finalMetricsMap = metricsMap;
        Map<UUID, Set<User>> finalAccountantsMap = accountantsMap;
        List<ClientListItemResponse> content = clientPage.getContent().stream()
                .map(client -> toClientListItem(client, finalMetricsMap.get(client.getId()), finalAccountantsMap.get(client.getId())))
                .toList();

        return new PageResponse<>(content, clientPage.getNumber(), clientPage.getSize(),
                clientPage.getTotalElements(), clientPage.getTotalPages());
    }

    private ClientListItemResponse toClientListItem(Client client, ClientTaskMetrics metrics, Set<User> accountants) {
        String clientName = clientMapper.computeClientName(client);

        List<String> accountantNames = accountants != null
                ? accountants.stream()
                    .map(UserDisplayUtil::formatDisplayName)
                    .toList()
                : List.of();

        long totalTasks = metrics != null ? metrics.getTotalTasks() : 0;
        long pendingTasks = metrics != null ? metrics.getPendingTasks() : 0;
        long overdueTasks = metrics != null ? metrics.getOverdueTasks() : 0;
        String nearestDeadline = metrics != null && metrics.getNearestDeadline() != null
                ? metrics.getNearestDeadline().atZone(ZoneOffset.UTC).toLocalDate().toString()
                : null;

        return new ClientListItemResponse(
                client.getId(), clientName, accountantNames,
                totalTasks, pendingTasks, overdueTasks,
                nearestDeadline, client.getStatus());
    }

    @Transactional(readOnly = true)
    public ClientSummaryResponse getClientSummary(UUID clientId) {
        Client client = clientRepository.findWithInfoAndAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        User currentUser = getCurrentUser();
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client.view.all"));

        if (!hasViewAll) {
            boolean isAssigned = client.getAccountants() != null
                    && client.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this client");
            }
        }

        ClientInfo info = client.getClientInfo();
        String displayName = clientMapper.computeClientName(client);

        String mreCode = info != null && info.getMainDetails() != null
                ? info.getMainDetails().mreCode() : null;

        String taxpayerClassification = null;
        ClientInformation ci = info != null ? info.getClientInformation() : null;
        if (ci != null && ci.birTaxCompliance() != null && ci.birTaxCompliance().taxpayerClassification() != null) {
            taxpayerClassification = ci.birTaxCompliance().taxpayerClassification().getDisplayName();
        }

        Set<User> accountants = client.getAccountants();
        Map<Boolean, List<String>> partitioned = (accountants == null ? Stream.<User>empty() : accountants.stream())
                .collect(Collectors.partitioningBy(
                        a -> a.getRole().getKey() == RoleKey.QTD,
                        Collectors.mapping(UserDisplayUtil::formatDisplayName, Collectors.toList())));

        return new ClientSummaryResponse(
                client.getId(), displayName, client.getStatus(),
                mreCode, taxpayerClassification,
                partitioned.get(false), partitioned.get(true));
    }

    @Transactional
    public void handoffClient(UUID clientId) {
        Client client = clientRepository.findWithInfoAccountantsAndUsersById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (client.getAccountants() == null || client.getAccountants().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accountants must be assigned before handoff");
        }

        client.setHandedOff(true);
        client.setStatus(ClientStatus.ACTIVE_CLIENT);
        clientRepository.save(client);

        String clientName = clientMapper.computeClientName(client);
        String message = "Client handed off: " + (clientName != null ? clientName : "Unknown");
        notificationService.notifyAll(
                new ArrayList<>(client.getAccountants()),
                NotificationType.CLIENT_HANDOFF,
                clientId, ReferenceType.CLIENT, message
        );
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (client.getStatus() != ClientStatus.ONBOARDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only onboarding clients can be deleted");
        }

        User currentUser = getCurrentUser();
        if (!client.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete clients you created");
        }

        fileService.deleteAllByClient(clientId);
        clientInfoRepository.deleteByClientId(clientId);
        clientRepository.delete(client);
    }

    @Transactional
    public void updateClientInfoSection(UUID clientId, String sectionKey, JsonNode body) {
        Client client = clientRepository.findWithInfoCreatorAndAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        boolean hasSubmittedTask = clientInfoTaskRepository.existsByClientIdAndStatusIn(
                clientId, List.of(ClientInfoTaskStatus.SUBMITTED));
        if (hasSubmittedTask) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client info cannot be edited while a review is pending");
        }

        User currentUser = getCurrentUser();
        boolean isCreator = client.getCreatedBy().getId().equals(currentUser.getId());
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "client_info.view.all"));
        boolean isAssigned = client.getAccountants() != null
                && client.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));

        if (!isCreator && !hasViewAll && !isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this client");
        }

        ClientInfo info = client.getClientInfo();
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client info not found");
        }

        try {
            switch (sectionKey) {
                case "mainDetails" -> {
                    MainDetailsPatchRequest req = objectMapper.treeToValue(body, MainDetailsPatchRequest.class);
                    info.setMainDetails(new MainDetails(req.mreCode(), req.commencementOfWork()));
                    updateAccountants(client, req.csdOosAccountantIds(), req.qtdAccountantId());
                }
                case "clientInformation" -> {
                    ClientInformation ci = objectMapper.treeToValue(body, ClientInformation.class);
                    info.setClientInformation(withComputedTaxpayerClassification(ci));
                }
                case "corporateOfficerInformation" -> info.setCorporateOfficerInformation(objectMapper.treeToValue(body, CorporateOfficerInformation.class));
                case "accessCredentials" -> info.setAccessCredentials(objectMapper.convertValue(body, new TypeReference<>() {
                }));
                case "scopeOfEngagement" -> info.setScopeOfEngagement(objectMapper.treeToValue(body, ScopeOfEngagementDetails.class));
                case "professionalFees" -> info.setProfessionalFees(objectMapper.convertValue(body, new TypeReference<>() {
                }));
                case "onboardingDetails" -> info.setOnboardingDetails(objectMapper.treeToValue(body, OnboardingDetails.class));
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section key: " + sectionKey);
            }
        } catch (tools.jackson.databind.DatabindException e) {
            String fieldPath = e.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."))
                    .replace(".[", "[");
            String detail = e.getOriginalMessage();
            throw new ClientInfoSectionException(sectionKey, fieldPath.isEmpty() ? null : fieldPath, detail);
        } catch (tools.jackson.core.JacksonException e) {
            throw new ClientInfoSectionException(sectionKey, null, e.getOriginalMessage());
        }

        clientInfoRepository.save(info);

    }

    @Transactional
    public void reassignAccountants(UUID clientId, ReassignClientAccountantsRequest request) {
        Client client = clientRepository.findWithAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Set<User> newAccountants = new HashSet<>();

        List<User> csdOos = userRepository.findAllById(request.csdOosAccountantIds());
        if (csdOos.size() != request.csdOosAccountantIds().stream().distinct().count()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more CSD/OOS accountant IDs are invalid");
        }
        for (User u : csdOos) {
            RoleKey key = u.getRole().getKey();
            if (key != RoleKey.CSD && key != RoleKey.OOS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User " + u.getId() + " is not a CSD or OOS accountant");
            }
        }
        newAccountants.addAll(csdOos);

        User qtd = userRepository.findById(request.qtdAccountantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "QTD accountant not found"));
        if (qtd.getRole().getKey() != RoleKey.QTD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User " + qtd.getId() + " is not a QTD accountant");
        }
        newAccountants.add(qtd);

        client.setAccountants(newAccountants);
        clientRepository.save(client);
    }

    private void updateAccountants(Client client, List<UUID> csdOosAccountantIds, UUID qtdAccountantId) {
        List<UUID> allIds = new ArrayList<>();
        if (csdOosAccountantIds != null) allIds.addAll(csdOosAccountantIds);
        if (qtdAccountantId != null) allIds.add(qtdAccountantId);

        if (allIds.isEmpty()) {
            client.setAccountants(new HashSet<>());
            clientRepository.save(client);
            return;
        }

        List<UUID> distinctIds = allIds.stream().distinct().toList();
        List<User> accountants = userRepository.findAllById(distinctIds);
        if (accountants.size() != distinctIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more accountant IDs are invalid");
        }

        client.setAccountants(new HashSet<>(accountants));
        clientRepository.save(client);
    }

    private ClientInfoHeaderResponse buildClientInfoHeaderResponse(Client client, ClientInfo info) {
        String displayName = clientMapper.computeClientName(client);

        String taxpayerClassification = null;
        ClientInformation ci = info.getClientInformation();
        if (ci != null && ci.birTaxCompliance() != null && ci.birTaxCompliance().taxpayerClassification() != null) {
            taxpayerClassification = ci.birTaxCompliance().taxpayerClassification().getDisplayName();
        }

        Set<User> accountants = client.getAccountants();
        Map<Boolean, List<AccountantListItemResponse>> partitioned = (accountants == null ? Stream.<User>empty() : accountants.stream())
                .map(userMapper::toAccountantListItemResponse)
                .collect(Collectors.partitioningBy(a -> a.roleKey() == RoleKey.QTD));

        // single query fetches latest task per type — replaces two separate queries
        List<ClientInfoTask> latestTasks = clientInfoTaskRepository
                .findLatestByClientIdGroupedByType(client.getId());

        Optional<ClientInfoTask> latestProfileUpdate = latestTasks.stream()
                .filter(t -> t.getType() == ClientInfoTaskType.PROFILE_UPDATE)
                .findFirst();
        boolean hasActiveTask = latestProfileUpdate.map(t -> t.getStatus() != ClientInfoTaskStatus.APPROVED).orElse(false);
        UUID activeTaskId = hasActiveTask ? latestProfileUpdate.get().getId() : null;
        ClientInfoTaskType activeTaskType = hasActiveTask ? latestProfileUpdate.get().getType() : null;
        ClientInfoTaskStatus lastReviewStatus = latestProfileUpdate.map(ClientInfoTask::getStatus).orElse(null);

        String pocEmail = client.getUsers() != null && !client.getUsers().isEmpty()
                ? client.getUsers().iterator().next().getEmail() : null;
        if (pocEmail == null) {
            CorporateOfficerInformation coi = info.getCorporateOfficerInformation();
            if (coi != null && coi.pointOfContact() != null) {
                pocEmail = coi.pointOfContact().emailAddress();
            }
        }

        boolean isProfileApproved = latestTasks.stream()
                .anyMatch(t -> t.getType() == ClientInfoTaskType.ONBOARDING
                        && t.getStatus() == ClientInfoTaskStatus.APPROVED);

        ClientOffboarding offboarding = client.getOffboarding();
        String offboardingAccountantName = offboarding != null && offboarding.getAccountant() != null
                ? UserDisplayUtil.formatDisplayName(offboarding.getAccountant())
                : null;

        return new ClientInfoHeaderResponse(
                displayName,
                taxpayerClassification,
                client.getStatus(),
                pocEmail,
                isProfileApproved,
                client.isHandedOff(),
                new ClientInfoHeaderResponse.AccountantsInfo(
                        partitioned.get(false), partitioned.get(true)),
                new ClientInfoHeaderResponse.TaskReviewInfo(
                        hasActiveTask, activeTaskId, activeTaskType, lastReviewStatus),
                new ClientInfoHeaderResponse.OffboardingInfo(
                        offboardingAccountantName,
                        offboarding != null ? offboarding.getEndOfEngagementDate() : null,
                        offboarding != null ? offboarding.getDeactivationDate() : null,
                        offboarding != null && offboarding.isTaxRecordsProtected(),
                        offboarding != null && offboarding.isEndOfEngagementLetterSent())
        );
    }

    private ClientInformation withComputedTaxpayerClassification(ClientInformation ci) {
        BirTaxComplianceDetails tc = ci.birTaxCompliance();
        if (tc == null) return ci;

        TaxpayerClassification classification = computeTaxpayerClassification(tc.grossSales());

        return new ClientInformation(
                ci.registeredName(), ci.tradeName(), ci.numberOfBranches(), ci.organizationType(),
                ci.birMainBranch(), ci.birBranches(),
                new BirTaxComplianceDetails(
                        tc.grossSales(), classification,
                        tc.topWithholding(), tc.dateClassifiedTopWithholding(), tc.incomeTaxRegime()
                ),
                ci.birComplianceBreakdown(), ci.dtiDetails(), ci.secDetails(),
                ci.sssDetails(), ci.philhealthDetails(), ci.hdmfDetails(), ci.cityHallDetails()
        );
    }

    @Transactional(readOnly = true)
    public ClientInfoHeaderResponse getMyClientInfoHeader() {
        UUID userId = getCurrentUser().getId();
        UUID clientId = clientRepository.findClientIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        Client client = clientRepository.findWithInfoAccountantsAndUsersById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        ClientInfo info = requireClientInfo(client);
        return buildClientInfoHeaderResponse(client, info);
    }

    @Transactional(readOnly = true)
    public String getMyClientInfoSection(String sectionKey) {
        UUID userId = getCurrentUser().getId();
        UUID clientId = clientRepository.findClientIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        String json = switch (sectionKey) {
            case "mainDetails" -> clientInfoRepository.findMainDetailsByClientId(clientId);
            case "clientInformation" -> clientInfoRepository.findClientInformationByClientId(clientId);
            case "corporateOfficerInformation" -> clientInfoRepository.findCorporateOfficerInformationByClientId(clientId);
            case "accessCredentials" -> clientInfoRepository.findAccessCredentialsByClientId(clientId);
            case "scopeOfEngagement" -> clientInfoRepository.findScopeOfEngagementByClientId(clientId);
            case "professionalFees" -> clientInfoRepository.findProfessionalFeesByClientId(clientId);
            case "onboardingDetails" -> clientInfoRepository.findOnboardingDetailsByClientId(clientId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section key: " + sectionKey);
        };

        if (json == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client info not found");
        }
        return json;
    }

    @Transactional(readOnly = true)
    public boolean hasEngagementLetter() {
        UUID userId = getCurrentUser().getId();
        return clientInfoRepository.existsEngagementLetterByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    @Transactional(readOnly = true)
    public List<FileReference> getEngagementLetters() {
        UUID userId = getCurrentUser().getId();
        String json = clientInfoRepository.findEngagementLettersByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (json == null || json.equals("null")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No engagement letters uploaded");
        }

        try {
            List<FileReference> letters = objectMapper.readValue(json, new TypeReference<>() {});
            if (letters == null || letters.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No engagement letters uploaded");
            }
            return letters;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse engagement letters");
        }
    }

    @Transactional
    public void updateClientStatus(UUID clientId, ClientStatusUpdateRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        client.setStatus(request.status());

        if (request.status() == ClientStatus.ACTIVE_CLIENT) {
            client.setOffboarding(null);
        }

        clientRepository.save(client);
    }

    private TaxpayerClassification computeTaxpayerClassification(List<GrossSalesEntry> grossSales) {
        if (grossSales == null || grossSales.isEmpty()) return null;

        BigDecimal amount = grossSales.stream()
                .max(Comparator.comparingInt(GrossSalesEntry::year))
                .map(GrossSalesEntry::amount)
                .orElse(null);

        if (amount == null) return null;

        if (amount.compareTo(BigDecimal.valueOf(3_000_000)) < 0) return TaxpayerClassification.MICRO;
        if (amount.compareTo(BigDecimal.valueOf(20_000_000)) < 0) return TaxpayerClassification.SMALL;
        if (amount.compareTo(BigDecimal.valueOf(1_000_000_000)) < 0) return TaxpayerClassification.MEDIUM;
        return TaxpayerClassification.LARGE;
    }
}

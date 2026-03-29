package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.ClientOffboardRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.SendEndOfEngagementLetterRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.TaxRecordsProtectionRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response.ClientOffboardingListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.mapper.ClientMapper;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.EndOfEngagementLetterTemplate;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.EndOfEngagementLetterTemplateService;
import com.taxrecordsportal.tax_records_portal_backend.common.util.TipTapHtmlRenderer;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.email.EmailService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationService;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.NotificationType;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.ReferenceType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ClientOffboardingService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;
    private final NotificationService notificationService;
    private final EndOfEngagementLetterTemplateService endOfEngagementLetterTemplateService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<ClientOffboardingListItemResponse> getOffboardingClients() {
        User currentUser = getCurrentUser();
        List<Client> clients = clientRepository.findByOffboardingAccountantIdAndStatus(
                currentUser.getId(), ClientStatus.OFFBOARDING);

        return clients.stream().map(this::toOffboardingListItem).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientOffboardingListItemResponse> getOffboardingClientsPaged(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Client> clientPage = clientRepository.findByOffboardingAccountantIdAndStatus(
                currentUser.getId(), ClientStatus.OFFBOARDING, pageable);

        return PageResponse.from(clientPage.map(this::toOffboardingListItem));
    }

    private ClientOffboardingListItemResponse toOffboardingListItem(Client client) {
        String name = clientMapper.computeClientName(client);
        String email = clientMapper.computeClientEmail(client);
        ClientOffboarding offboarding = client.getOffboarding();
        String offboardingAccountantName = offboarding != null && offboarding.getAccountant() != null
                ? UserDisplayUtil.formatDisplayName(offboarding.getAccountant())
                : null;
        return new ClientOffboardingListItemResponse(
                client.getId(), name, email, client.getStatus(),
                offboardingAccountantName,
                offboarding != null ? offboarding.getEndOfEngagementDate() : null,
                offboarding != null ? offboarding.getDeactivationDate() : null,
                offboarding != null && offboarding.isTaxRecordsProtected(),
                offboarding != null && offboarding.isEndOfEngagementLetterSent(),
                client.getCreatedAt(),
                client.getUpdatedAt());
    }

    @Transactional
    public void offboardClient(UUID clientId, ClientOffboardRequest request) {
        Client client = clientRepository.findWithInfoAndCreatorById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (client.getStatus() != ClientStatus.ACTIVE_CLIENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active clients can be offboarded");
        }

        User oosAccountant = userRepository.findById(request.oosAccountantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accountant not found"));

        if (oosAccountant.getRole().getKey() != RoleKey.OOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offboarding accountant must be OOS");
        }

        client.setStatus(ClientStatus.OFFBOARDING);

        ClientOffboarding offboarding = ensureOffboarding(client);
        offboarding.setAccountant(oosAccountant);
        offboarding.setEndOfEngagementDate(request.endOfEngagementDate());
        offboarding.setDeactivationDate(request.deactivationDate());

        clientRepository.save(client);

        String clientName = clientMapper.computeClientName(client);
        String message = "Offboarding assigned: " + (clientName != null ? clientName : "Unknown");
        notificationService.notifyAll(
                List.of(oosAccountant),
                NotificationType.OFFBOARDING_ASSIGNED,
                clientId, ReferenceType.CLIENT, message);
    }

    @Transactional
    public void updateTaxRecordsProtection(UUID clientId, TaxRecordsProtectionRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        ClientOffboarding offboarding = ensureOffboarding(client);
        offboarding.setTaxRecordsProtected(request.protectTaxRecords());
        clientRepository.save(client);
    }

    @Transactional
    public void sendEndOfEngagementLetter(UUID clientId, SendEndOfEngagementLetterRequest request) {
        Client client = clientRepository.findWithInfoAndUsersById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        User currentUser = getCurrentUser();
        boolean isManager = currentUser.getRole().getKey() == RoleKey.MANAGER;
        ClientOffboarding offboarding = client.getOffboarding();
        boolean isOffboardingAccountant = offboarding != null && offboarding.getAccountant() != null
                && offboarding.getAccountant().getId().equals(currentUser.getId());

        if (!isManager && !isOffboardingAccountant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the offboarding accountant or a manager can send the engagement letter");
        }

        EndOfEngagementLetterTemplate template = endOfEngagementLetterTemplateService.findOrThrow(request.templateId());

        List<String> recipientEmails = client.getUsers() != null
                ? client.getUsers().stream()
                    .map(User::getEmail)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()
                : List.of();

        if (recipientEmails.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No client email recipients found");
        }

        String clientName = clientMapper.computeClientName(client);
        String subject = "End of Engagement Letter" + (clientName != null ? " - " + clientName : "");

        String htmlBody = TipTapHtmlRenderer.render(template.getBody());

        for (String email : recipientEmails) {
            emailService.sendEndOfEngagementLetterEmail(email, subject, htmlBody);
        }

        ensureOffboarding(client).setEndOfEngagementLetterSent(true);
        clientRepository.save(client);
    }

    private ClientOffboarding ensureOffboarding(Client client) {
        if (client.getOffboarding() == null) {
            ClientOffboarding offboarding = new ClientOffboarding();
            offboarding.setClient(client);
            client.setOffboarding(offboarding);
        }
        return client.getOffboarding();
    }
}

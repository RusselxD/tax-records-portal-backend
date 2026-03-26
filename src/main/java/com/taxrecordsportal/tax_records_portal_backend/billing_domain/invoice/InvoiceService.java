package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request.InvoiceCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.request.ReceivePaymentRequest;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response.*;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.mapper.InvoiceMapper;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_payment.InvoicePayment;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_payment.InvoicePaymentRepository;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.InvoiceTerm;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.InvoiceTermRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.common.dto.PageResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import com.taxrecordsportal.tax_records_portal_backend.common.R2StorageService;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.email.EmailService;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.sendgrid.helpers.mail.objects.Attachments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InvoiceTermRepository invoiceTermRepository;
    private final ClientRepository clientRepository;
    private final FileRepository fileRepository;
    private final R2StorageService r2StorageService;
    private final EmailService emailService;
    private final InvoiceMapper invoiceMapper;

    @Transactional(readOnly = true)
    public PageResponse<BillingClientListItemResponse> getBillingClients(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BillingClientProjection> result = invoiceRepository.findBillingClientSummaries(search, pageable);
        return PageResponse.from(result.map(p -> {
            String name = buildDisplayName(p.getRegisteredName(), p.getTradeName());
            return new BillingClientListItemResponse(
                    p.getClientId(), name,
                    p.getTotalInvoices(), p.getUnpaidInvoices(),
                    p.getPartiallyPaidInvoices(), p.getFullyPaidInvoices(),
                    p.getTotalAmountDue(), p.getTotalBalance()
            );
        }));
    }

    private String buildDisplayName(String registeredName, String tradeName) {
        if (registeredName != null && tradeName != null) {
            return registeredName + " (" + tradeName + ")";
        }
        if (registeredName != null) return registeredName;
        return tradeName;
    }

    public PageResponse<InvoiceListItemResponse> getInvoices(UUID clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Invoice> result;
        if (clientId != null) {
            result = invoiceRepository.findByClientIdOrderByCreatedAtDesc(clientId, pageable);
        } else {
            result = invoiceRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Map<UUID, String> clientNames = batchFetchClientNames(result.getContent());
        Set<UUID> clientsWithEmail = batchCheckEmailRecipients(result.getContent());
        return PageResponse.from(result.map(inv -> {
            UUID cid = inv.getClient().getId();
            return invoiceMapper.toListItem(inv, clientNames.getOrDefault(cid, null), clientsWithEmail.contains(cid));
        }));
    }

    private boolean hasEmailRecipients(UUID clientId) {
        return !invoiceRepository.findClientIdsWithEmail(List.of(clientId)).isEmpty();
    }

    private Set<UUID> batchCheckEmailRecipients(List<Invoice> invoices) {
        List<UUID> clientIds = invoices.stream()
                .map(inv -> inv.getClient().getId())
                .distinct()
                .toList();
        if (clientIds.isEmpty()) return Set.of();
        return new HashSet<>(invoiceRepository.findClientIdsWithEmail(clientIds));
    }

    private String fetchClientName(UUID clientId) {
        return invoiceRepository.findClientNamesByIds(List.of(clientId)).stream()
                .findFirst()
                .map(p -> buildDisplayName(p.getRegisteredName(), p.getTradeName()))
                .orElse(null);
    }

    private Map<UUID, String> batchFetchClientNames(List<Invoice> invoices) {
        List<UUID> clientIds = invoices.stream()
                .map(inv -> inv.getClient().getId())
                .distinct()
                .toList();
        if (clientIds.isEmpty()) return Map.of();

        return invoiceRepository.findClientNamesByIds(clientIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ClientNameProjection::getClientId,
                        p -> buildDisplayName(p.getRegisteredName(), p.getTradeName()),
                        (a, b) -> a
                ));
    }

    @Transactional(readOnly = true)
    public List<ClientOutstandingInvoiceResponse> getMyOutstandingInvoices() {
        UUID clientId = getMyClientId();
        List<Invoice> invoices = invoiceRepository.findByClientIdAndStatusInOrderByDueDateAsc(
                clientId, List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID));
        return invoices.stream().map(invoiceMapper::toOutstandingItem).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientInvoiceListItemResponse> getMyInvoices(int page, int size) {
        UUID clientId = getMyClientId();
        Page<Invoice> result = invoiceRepository.findByClientIdOrderByCreatedAtDesc(
                clientId, PageRequest.of(page, size));
        return PageResponse.from(result.map(invoiceMapper::toClientListItem));
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse getMyInvoice(UUID invoiceId) {
        UUID clientId = getMyClientId();
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (!invoice.getClient().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        String clientName = fetchClientName(clientId);
        boolean hasEmail = hasEmailRecipients(clientId);
        return invoiceMapper.toDetail(invoice, clientName, hasEmail);
    }

    private UUID getMyClientId() {
        UUID userId = getCurrentUser().getId();
        return clientRepository.findClientIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No client associated with this account"));
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse getInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        String clientName = fetchClientName(invoice.getClient().getId());
        boolean hasEmail = hasEmailRecipients(invoice.getClient().getId());
        return invoiceMapper.toDetail(invoice, clientName, hasEmail);
    }

    @Transactional
    public InvoiceDetailResponse createInvoice(InvoiceCreateRequest request) {
        if (invoiceRepository.existsByInvoiceNumber(request.invoiceNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice number already exists.");
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        InvoiceTerm terms = invoiceTermRepository.findById(request.termsId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice term not found"));

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setTerms(terms);
        invoice.setDueDate(request.invoiceDate().plusDays(terms.getDays()));
        invoice.setDescription(request.description());
        invoice.setAmountDue(request.amountDue());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setAttachments(request.attachments());
        invoice.setCreatedBy(getCurrentUser());

        Invoice saved = invoiceRepository.save(invoice);
        saved.setPayments(Collections.emptyList());
        String clientName = fetchClientName(client.getId());
        boolean hasEmail = hasEmailRecipients(client.getId());
        return invoiceMapper.toDetail(saved, clientName, hasEmail);
    }

    @Transactional
    public void sendInvoiceEmail(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        Set<String> emails = collectClientEmails(invoice.getClient().getId());

        List<Attachments> emailAttachments = buildEmailAttachments(invoice);

        for (String email : emails) {
            emailService.sendInvoiceNotification(
                    email,
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceDate(),
                    invoice.getDueDate(),
                    invoice.getDescription(),
                    invoice.getAmountDue(),
                    emailAttachments
            );
        }

        invoice.setEmailSent(true);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void sendPaymentEmail(UUID invoiceId, UUID paymentId) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        InvoicePayment payment = invoice.getPayments().stream()
                .filter(p -> p.getId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        Set<String> emails = collectClientEmails(invoice.getClient().getId());

        BigDecimal balance = invoice.getAmountDue().subtract(invoiceMapper.sumPayments(invoice));

        for (String email : emails) {
            emailService.sendPaymentReceiptNotification(
                    email,
                    invoice.getInvoiceNumber(),
                    payment.getDate(),
                    payment.getAmount(),
                    balance,
                    invoice.getAmountDue()
            );
        }

        payment.setEmailSent(true);
        invoicePaymentRepository.save(payment);
    }

    private Set<String> collectClientEmails(UUID clientId) {
        Client client = clientRepository.findWithInfoAndUsersById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Set<String> emails = new LinkedHashSet<>();

        if (client.getClientInfo() != null && client.getClientInfo().getCorporateOfficerInformation() != null) {
            var poc = client.getClientInfo().getCorporateOfficerInformation().pointOfContact();
            if (poc != null && poc.emailAddress() != null && !poc.emailAddress().isBlank()) {
                emails.add(poc.emailAddress().trim().toLowerCase());
            }
        }

        if (client.getUsers() != null) {
            for (User user : client.getUsers()) {
                if (user.getEmail() != null) {
                    emails.add(user.getEmail().trim().toLowerCase());
                }
            }
        }

        if (emails.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No email addresses found for this client.");
        }

        return emails;
    }

    private List<Attachments> buildEmailAttachments(Invoice invoice) {
        if (invoice.getAttachments() == null || invoice.getAttachments().isEmpty()) {
            return List.of();
        }

        List<Attachments> result = new ArrayList<>();
        for (FileReference ref : invoice.getAttachments()) {
            FileEntity file = fileRepository.findById(ref.id()).orElse(null);
            if (file == null) continue;

            try (var inputStream = r2StorageService.download(file.getUrl())) {
                byte[] bytes = inputStream.readAllBytes();
                String encoded = java.util.Base64.getEncoder().encodeToString(bytes);

                Attachments attachment = new Attachments();
                attachment.setContent(encoded);
                attachment.setFilename(file.getName());
                attachment.setDisposition("attachment");
                result.add(attachment);
            } catch (Exception e) {
                log.warn("Failed to attach file {} to email: {}", ref.id(), e.getMessage());
            }
        }
        return result;
    }

    @Transactional
    public void deleteInvoice(UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        invoiceRepository.deleteById(invoiceId);
    }

    @Transactional
    public void voidInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.isVoided()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is already voided.");
        }

        invoice.setVoided(true);
        invoice.setStatus(InvoiceStatus.VOID);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoicePaymentResponse receivePayment(UUID invoiceId, ReceivePaymentRequest request) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.isVoided()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot receive payment on a voided invoice.");
        }

        if (invoice.getStatus() == InvoiceStatus.FULLY_PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is already fully paid.");
        }

        InvoicePayment payment = new InvoicePayment();
        payment.setInvoice(invoice);
        payment.setDate(request.date());
        payment.setAmount(request.amount());
        payment.setAttachments(request.attachments());

        InvoicePayment saved = invoicePaymentRepository.save(payment);

        // Recompute status
        invoice.getPayments().add(saved);
        recomputeStatus(invoice);
        invoiceRepository.save(invoice);

        return invoiceMapper.toPaymentResponse(saved);
    }

    private void recomputeStatus(Invoice invoice) {
        if (invoice.isVoided()) {
            invoice.setStatus(InvoiceStatus.VOID);
            return;
        }

        BigDecimal paid = invoiceMapper.sumPayments(invoice);
        if (paid.compareTo(invoice.getAmountDue()) >= 0) {
            invoice.setStatus(InvoiceStatus.FULLY_PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.UNPAID);
        }
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.dto.request.CreateClientNoticeRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.dto.response.ClientNoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientNoticeService {

    private final ClientNoticeRepository clientNoticeRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public ClientNoticeResponse create(UUID clientId, CreateClientNoticeRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        ClientNotice notice = new ClientNotice();
        notice.setClient(client);
        notice.setType(request.type());
        notice.setContent(request.content());

        ClientNotice saved = clientNoticeRepository.save(notice);
        return new ClientNoticeResponse(saved.getId(), saved.getType(), saved.getContent());
    }

    @Transactional(readOnly = true)
    public List<ClientNoticeResponse> getByClientId(UUID clientId) {
        return clientNoticeRepository.findByClientIdOrderByIdDesc(clientId)
                .stream()
                .map(n -> new ClientNoticeResponse(n.getId(), n.getType(), n.getContent()))
                .toList();
    }

    @Transactional
    public void delete(UUID clientId, Integer noticeId) {
        ClientNotice notice = clientNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found"));

        if (!notice.getClient().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found");
        }

        clientNoticeRepository.delete(notice);
    }
}

package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.dto.request.ClientConsultationConfigUpsertRequest;
import com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config.dto.response.ClientConsultationConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientConsultationConfigService {

    private final ClientConsultationConfigRepository configRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public ClientConsultationConfigResponse getConfig(UUID clientId) {
        ClientConsultationConfig config = configRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation config not found for this client"));
        return new ClientConsultationConfigResponse(config.getClientId(), config.getIncludedHours(), config.getExcessRate());
    }

    @Transactional
    public ClientConsultationConfigResponse upsertConfig(UUID clientId, ClientConsultationConfigUpsertRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        ClientConsultationConfig config = configRepository.findByClientId(clientId)
                .orElseGet(() -> {
                    ClientConsultationConfig c = new ClientConsultationConfig();
                    c.setClient(client);
                    return c;
                });

        config.setIncludedHours(request.includedHours());
        config.setExcessRate(request.excessRate());
        configRepository.save(config);

        return new ClientConsultationConfigResponse(config.getClientId(), config.getIncludedHours(), config.getExcessRate());
    }
}

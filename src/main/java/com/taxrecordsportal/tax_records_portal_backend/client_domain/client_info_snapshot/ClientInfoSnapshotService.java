package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_snapshot.dto.response.ClientInfoSnapShotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientInfoSnapshotService {

    private final ClientInfoSnapshotRepository clientInfoSnapshotRepository;

    @Transactional(readOnly = true)
    public ClientInfoSnapShotResponse getClientInfoSnapshot(UUID clientId) {
        ClientInfoSnapshot snapshot = clientInfoSnapshotRepository.findTopByClientIdOrderByCreatedAtDesc(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client info snapshot not found"));

        return new ClientInfoSnapShotResponse(
                snapshot.getMainDetails(),
                snapshot.getClientInformation(),
                snapshot.getCorporateOfficerInformation(),
                snapshot.getAccessCredentials(),
                snapshot.getScopeOfEngagement(),
                snapshot.getProfessionalFees(),
                snapshot.getOnboardingDetails(),
                snapshot.getCreatedAt()
        );
    }
}

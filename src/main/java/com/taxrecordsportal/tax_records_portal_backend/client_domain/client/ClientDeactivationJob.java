package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.DateUtil.ZONE_PH;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientDeactivationJob {

    private final ClientRepository clientRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Manila")
    @Transactional
    public void deactivateClients() {
        LocalDate today = LocalDate.now(ZONE_PH);

        var clients = clientRepository.findByStatusAndDeactivationDateLessThanEqual(
                ClientStatus.OFFBOARDING, today);

        if (clients.isEmpty()) return;

        for (var client : clients) {
            client.setStatus(ClientStatus.INACTIVE_CLIENT);

            if (client.getUsers() != null) {
                client.getUsers().forEach(user -> user.setStatus(UserStatus.DEACTIVATED));
            }
        }

        clientRepository.saveAll(clients);
        log.info("Deactivated {} client(s) with deactivation date <= {}", clients.size(), today);
    }
}

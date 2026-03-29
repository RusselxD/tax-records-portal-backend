package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request.ClientActivateRequest;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.email.EmailService;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.TokenType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.UserToken;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ClientActivationService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserTokenRepository userTokenRepository;
    private final EmailService emailService;

    @Value("${application.security.jwt.activation-token-expiration}")
    private long activationTokenExpiration;

    @Transactional
    public void activateClient(UUID clientId, ClientActivateRequest request) {
        Client client = clientRepository.findWithAccountantsById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        User currentUser = getCurrentUser();
        boolean hasClientCreate = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("client.create"));
        boolean isAssigned = client.getAccountants() != null
                && client.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
        if (!hasClientCreate && !isAssigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        Role clientRole = roleRepository.findByKey(RoleKey.CLIENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Client role not found"));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setRole(clientRole);
        user.setStatus(UserStatus.PENDING);
        user.setClient(client);
        User savedUser = userRepository.save(user);

        UserToken activationToken = new UserToken();
        activationToken.setUser(savedUser);
        activationToken.setToken(UUID.randomUUID().toString());
        activationToken.setType(TokenType.ACCOUNT_ACTIVATION);
        activationToken.setExpiresAt(Instant.now().plusMillis(activationTokenExpiration));
        userTokenRepository.save(activationToken);

        emailService.sendActivationEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                activationToken.getToken()
        );
    }
}

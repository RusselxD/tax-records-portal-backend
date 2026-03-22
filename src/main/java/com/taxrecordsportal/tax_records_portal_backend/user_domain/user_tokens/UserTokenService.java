package com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.dto.ActivateAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ActivateAccountResponse validateActivationToken(String token) {
        UserToken userToken = userTokenRepository.findByTokenAndType(token, TokenType.ACCOUNT_ACTIVATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid activation token."));

        if (userToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token has expired.");
        }

        return new ActivateAccountResponse(true, userToken.getUser().getEmail());
    }
}

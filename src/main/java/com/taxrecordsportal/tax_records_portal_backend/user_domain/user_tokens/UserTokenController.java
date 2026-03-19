package com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.dto.ActivateAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tokens")
public class UserTokenController {

    private final UserTokenService userTokenService;

    @GetMapping("/verify/activation-token")
    public ResponseEntity<ActivateAccountResponse> verifyActivationToken (
        @RequestParam String token
    ) {
        return ResponseEntity.ok(userTokenService.validateActivationToken(token));
    }
}

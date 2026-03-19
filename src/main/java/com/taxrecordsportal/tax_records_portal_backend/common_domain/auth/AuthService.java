package com.taxrecordsportal.tax_records_portal_backend.common_domain.auth;

import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.AuthResponse;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.LoginRequest;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.RefreshRequest;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.SetPasswordRequest;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.security.JwtService;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.TokenType;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.UserToken;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user_tokens.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(LoginRequest request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userRepository.findByEmail(request.email()).orElseThrow();

        return getAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {

        UserToken refreshToken = userTokenRepository.findByTokenAndType(request.refreshToken(), TokenType.REFRESH_TOKEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            userTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        // return new access token, keep the same refresh token
        return new AuthResponse(newAccessToken, refreshToken.getToken(), "Bearer");
    }

    @Transactional
    public AuthResponse setPassword(SetPasswordRequest request) {
        UserToken activationToken = userTokenRepository.findByTokenAndType(request.token(), TokenType.ACCOUNT_ACTIVATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid activation token."));

        if (activationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token has expired.");
        }

        User user = activationToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        userTokenRepository.delete(activationToken);

        return getAuthResponse(user);
    }

    @NonNull
    private AuthResponse getAuthResponse(User user) {
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = UUID.randomUUID().toString();

        userTokenRepository.deleteByUserAndType(user, TokenType.REFRESH_TOKEN);

        UserToken refreshTokenEntity = new UserToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(newRefreshToken);
        refreshTokenEntity.setType(TokenType.REFRESH_TOKEN);
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        userTokenRepository.saveAndFlush(refreshTokenEntity);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer");
    }

}

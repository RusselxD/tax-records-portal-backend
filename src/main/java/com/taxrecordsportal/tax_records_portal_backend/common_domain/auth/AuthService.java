package com.taxrecordsportal.tax_records_portal_backend.common_domain.auth;

import com.taxrecordsportal.tax_records_portal_backend.common_domain.auth.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.common_domain.email.EmailService;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long PASSWORD_RESET_EXPIRATION_MS = 15 * 60 * 1000; // 15 minutes

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public AuthResponse login(LoginRequest request) {

        org.springframework.security.core.Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // User was already loaded by UserDetailsServiceImpl during authenticate() — reuse from return value
        User user = (User) authentication.getPrincipal();

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

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isEmpty()) return;

        User user = optionalUser.get();
        if (user.getStatus() != UserStatus.ACTIVE) return;

        userTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);

        UserToken token = new UserToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setType(TokenType.PASSWORD_RESET);
        token.setExpiresAt(Instant.now().plusMillis(PASSWORD_RESET_EXPIRATION_MS));
        userTokenRepository.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token.getToken());
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        UserToken token = userTokenRepository.findByTokenAndType(request.token(), TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token."));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            userTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        userTokenRepository.delete(token);

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

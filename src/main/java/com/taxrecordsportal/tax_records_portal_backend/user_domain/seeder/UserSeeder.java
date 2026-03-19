package com.taxrecordsportal.tax_records_portal_backend.user_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.common_domain.email.EmailService;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePosition;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePositionRepository;
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
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Order(2)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeePositionRepository employeePositionRepository;
    private final UserTokenRepository userTokenRepository;
    private final EmailService emailService;

    @Value("${application.security.jwt.activation-token-expiration}")
    private long activationTokenExpiration;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Role managerRole = roleRepository.findByKey(RoleKey.MANAGER)
                .orElseThrow(() -> new RuntimeException("Manager role not found. Ensure RolePermissionSeeder runs first."));

        EmployeePosition operationsMre = employeePositionRepository.findByName("Operations MRE")
                .orElseThrow(() -> new RuntimeException("Operations MRE position not found. Ensure EmployeePositionSeeder runs first."));

        User manager = new User();
        manager.setFirstName("John");
        manager.setLastName("Doe");
        manager.setEmail("russelcabigquez8@gmail.com");
        manager.setRole(managerRole);
        manager.setPosition(operationsMre);
        manager.setStatus(UserStatus.PENDING);
        User savedUser = userRepository.save(manager);

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

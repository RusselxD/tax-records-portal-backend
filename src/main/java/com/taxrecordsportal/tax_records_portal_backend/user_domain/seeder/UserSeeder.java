package com.taxrecordsportal.tax_records_portal_backend.user_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePosition;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePositionRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserRepository;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(2)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeePositionRepository employeePositionRepository;
    private final PasswordEncoder passwordEncoder;

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
        manager.setStatus(UserStatus.ACTIVE);
        manager.setPasswordHash(passwordEncoder.encode("Admin123!"));
        userRepository.save(manager);
    }
}

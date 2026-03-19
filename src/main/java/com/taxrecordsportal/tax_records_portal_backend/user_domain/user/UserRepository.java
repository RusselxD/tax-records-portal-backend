package com.taxrecordsportal.tax_records_portal_backend.user_domain.user;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"role", "role.permissions", "position"})
    Optional<User> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"role", "role.permissions", "position"})
    Optional<User> findById(UUID id);

    @EntityGraph(attributePaths = {"role"})
    List<User> findAllByRoleNot(Role role);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByRole_KeyInAndStatus(List<RoleKey> roleKeys, UserStatus status);

    List<User> findByRoleKey(RoleKey roleKey);
}

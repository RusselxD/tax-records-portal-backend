package com.taxrecordsportal.tax_records_portal_backend.user_domain.user;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.RoleKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.key = :roleKey AND u.createdAt >= :since")
    long countByRoleKeyAndCreatedAtSince(@Param("roleKey") RoleKey roleKey, @Param("since") Instant since);
}

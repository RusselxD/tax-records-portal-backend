package com.taxrecordsportal.tax_records_portal_backend.user_domain.role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByKey(RoleKey key);

    List<Role> findAllByKeyNot(RoleKey key);
}

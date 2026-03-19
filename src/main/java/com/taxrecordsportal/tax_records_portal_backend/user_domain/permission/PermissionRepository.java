package com.taxrecordsportal.tax_records_portal_backend.user_domain.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByName(String name);
}
package com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeePositionRepository extends JpaRepository<EmployeePosition, Integer> {
    boolean existsByName(String name);
    Optional<EmployeePosition> findByName(String name);
}

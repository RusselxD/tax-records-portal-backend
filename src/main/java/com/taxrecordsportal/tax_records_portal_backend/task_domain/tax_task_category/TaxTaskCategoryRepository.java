package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxTaskCategoryRepository extends JpaRepository<TaxTaskCategory, Integer> {

    boolean existsByName(String name);
}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaxTaskNameRepository extends JpaRepository<TaxTaskName, Integer> {

    List<TaxTaskName> findBySubCategoryId(Integer subCategoryId);

    boolean existsByNameAndSubCategoryId(String name, Integer subCategoryId);

    @EntityGraph(attributePaths = {"subCategory"})
    @Query("SELECT tn FROM TaxTaskName tn")
    List<TaxTaskName> findAllWithSubCategory();
}

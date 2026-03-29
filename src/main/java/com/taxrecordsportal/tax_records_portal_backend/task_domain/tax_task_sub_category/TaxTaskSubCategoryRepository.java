package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaxTaskSubCategoryRepository extends JpaRepository<TaxTaskSubCategory, Integer> {

    List<TaxTaskSubCategory> findByCategoryId(Integer categoryId);

    boolean existsByNameAndCategoryId(String name, Integer categoryId);

    boolean existsByCategoryId(Integer categoryId);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT sc FROM TaxTaskSubCategory sc")
    List<TaxTaskSubCategory> findAllWithCategory();
}

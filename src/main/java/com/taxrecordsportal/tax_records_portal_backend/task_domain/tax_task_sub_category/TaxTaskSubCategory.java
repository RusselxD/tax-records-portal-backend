package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tax_task_sub_categories")
public class TaxTaskSubCategory {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TaxTaskCategory category;

    @Column(nullable = false)
    private String name;
}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tax_task_sub_categories")
public class TaxTaskSubCategory {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TaxTaskCategory category;

    @Column(nullable = false)
    private String name;
}

package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
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
@Table(name = "tax_task_names")
public class TaxTaskName {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private TaxTaskSubCategory subCategory;

    @Column(nullable = false)
    private String name;
}

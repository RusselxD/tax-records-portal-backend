package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tax_task_categories")
public class TaxTaskCategory {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;
}

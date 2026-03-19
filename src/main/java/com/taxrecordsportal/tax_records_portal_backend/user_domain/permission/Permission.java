package com.taxrecordsportal.tax_records_portal_backend.user_domain.permission;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @ManyToMany(mappedBy = "permissions", fetch = LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Role> roles;
}

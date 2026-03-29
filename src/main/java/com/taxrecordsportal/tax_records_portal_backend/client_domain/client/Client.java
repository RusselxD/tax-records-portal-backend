package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "clients", indexes = {
        @Index(name = "idx_clients_status", columnList = "status"),
        @Index(name = "idx_clients_created_by", columnList = "created_by")
})
public class Client {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private Long version;

    @JsonIgnore
    @OneToMany(mappedBy = "client", fetch = LAZY)
    @BatchSize(size = 20)
    private Set<User> users;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClientStatus status;

    @Column(name = "handed_off", nullable = false)
    private boolean handedOff = false;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = LAZY)
    private ClientOffboarding offboarding;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToOne(mappedBy = "client", fetch = LAZY)
    private ClientInfo clientInfo;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "client_accountants",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            indexes = {
                    @Index(name = "idx_client_accountants_client_id", columnList = "client_id"),
                    @Index(name = "idx_client_accountants_user_id", columnList = "user_id")
            }
    )
    @BatchSize(size = 20)
    private Set<User> accountants;
}
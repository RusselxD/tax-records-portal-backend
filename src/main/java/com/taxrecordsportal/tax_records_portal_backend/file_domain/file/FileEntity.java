package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "files", indexes = {
        @Index(name = "idx_files_client_id", columnList = "client_id")
})
public class FileEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private java.util.UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false)
    private String url;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
}

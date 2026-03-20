package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_notices", indexes = {
        @Index(name = "idx_client_notices_client_id", columnList = "client_id")
})
public class ClientNotice {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NoticeType type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
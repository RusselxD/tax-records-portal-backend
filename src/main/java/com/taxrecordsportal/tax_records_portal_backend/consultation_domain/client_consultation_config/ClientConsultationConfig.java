package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.client_consultation_config;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "client_consultation_configs")
public class ClientConsultationConfig {

    @Id
    @EqualsAndHashCode.Include
    private UUID clientId;

    @OneToOne(fetch = LAZY)
    @MapsId
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "included_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal includedHours;

    @Column(name = "excess_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal excessRate;
}

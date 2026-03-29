package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "client_offboarding")
@Getter
@Setter
@NoArgsConstructor
public class ClientOffboarding {

    @Id
    @Column(name = "client_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accountant_id")
    private User accountant;

    @Column(name = "end_of_engagement_date")
    private LocalDate endOfEngagementDate;

    @Column(name = "deactivation_date")
    private LocalDate deactivationDate;

    @Column(name = "tax_records_protected", nullable = false)
    private boolean taxRecordsProtected;

    @Column(name = "end_of_engagement_letter_sent", nullable = false)
    private boolean endOfEngagementLetterSent;
}

package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_payment.InvoicePayment;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_term.InvoiceTerm;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_client_id", columnList = "client_id"),
        @Index(name = "idx_invoices_status", columnList = "status"),
        @Index(name = "idx_invoices_due_date", columnList = "due_date"),
        @Index(name = "idx_invoices_client_status", columnList = "client_id, status")
})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "terms_id", nullable = false)
    private InvoiceTerm terms;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "description")
    private String description;

    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @Column(name = "attachments", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<FileReference> attachments;

    @OneToMany(mappedBy = "invoice", fetch = LAZY)
    @BatchSize(size = 20)
    private List<InvoicePayment> payments;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
